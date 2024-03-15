// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.renamer;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.extern.IIdentifierRenamer;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructContext;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.gen.FieldDescriptor;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.struct.gen.NewClassNameBuilder;
import org.jetbrains.java.decompiler.util.VBStyleCollection;

import java.io.IOException;
import java.util.*;

public class IdentifierConverter implements NewClassNameBuilder {
  private final StructContext context;
  private final IIdentifierRenamer helper;
  private final PoolInterceptor interceptor;
  private List<ClassWrapperNode> rootClasses = new ArrayList<>();
  private List<ClassWrapperNode> rootInterfaces = new ArrayList<>();
  private Map<String, Map<String, String>> interfaceNameMaps = new LinkedHashMap<>();

  public IdentifierConverter(StructContext context, IIdentifierRenamer helper, PoolInterceptor interceptor) {
    this.context = context;
    this.helper = helper;
    this.interceptor = interceptor;
  }

  public void rename() {
    try {
      buildInheritanceTree();
      renameAllClasses();
      renameInterfaces();
      renameClasses();
      context.reloadContext();
    }
    catch (IOException ex) {
      throw new RuntimeException("Renaming failed with exception!", ex);
    }
  }

  private void renameClasses() {
    List<ClassWrapperNode> lstClasses = getReversePostOrderListIterative(rootClasses);
    Map<String, Map<String, String>> classNameMaps = new LinkedHashMap<>();

    for (ClassWrapperNode node : lstClasses) {
      StructClass cl = node.getClassStruct();
      Map<String, String> names = new LinkedHashMap<>();

      // merge information on super class
      if (cl.superClass != null) {
        Map<String, String> mapClass = classNameMaps.get(cl.superClass.getString());
        if (mapClass != null) {
          names.putAll(mapClass);
        }
      }

      // merge information on interfaces
      for (String ifName : cl.getInterfaceNames()) {
        Map<String, String> mapInt = interfaceNameMaps.get(ifName);
        if (mapInt != null) {
          names.putAll(mapInt);
        }
        else {
          StructClass clintr = context.getClass(ifName);
          if (clintr != null) {
            names.putAll(processExternalInterface(clintr));
          }
        }
      }

      renameClassIdentifiers(cl, names);

      if (!node.getSubclasses().isEmpty()) {
        classNameMaps.put(cl.qualifiedName, names);
      }
    }
  }

  private Map<String, String> processExternalInterface(StructClass cl) {
    Map<String, String> names = new LinkedHashMap<>();

    for (String ifName : cl.getInterfaceNames()) {
      Map<String, String> mapInt = interfaceNameMaps.get(ifName);
      if (mapInt != null) {
        names.putAll(mapInt);
      }
      else {
        StructClass clintr = context.getClass(ifName);
        if (clintr != null) {
          names.putAll(processExternalInterface(clintr));
        }
      }
    }

    renameClassIdentifiers(cl, names);

    return names;
  }

  private void renameInterfaces() {
    List<ClassWrapperNode> lstInterfaces = getReversePostOrderListIterative(rootInterfaces);
    Map<String, Map<String, String>> interfaceNameMaps = new LinkedHashMap<>();

    // rename methods and fields
    for (ClassWrapperNode node : lstInterfaces) {

      StructClass cl = node.getClassStruct();
      Map<String, String> names = new LinkedHashMap<>();

      // merge information on super interfaces
      for (String ifName : cl.getInterfaceNames()) {
        Map<String, String> mapInt = interfaceNameMaps.get(ifName);
        if (mapInt != null) {
          names.putAll(mapInt);
        }
      }

      renameClassIdentifiers(cl, names);

      interfaceNameMaps.put(cl.qualifiedName, names);
    }

    this.interfaceNameMaps = interfaceNameMaps;
  }

  private void renameAllClasses() {
    // order not important
    List<ClassWrapperNode> lstAllClasses = new ArrayList<>(getReversePostOrderListIterative(rootInterfaces));
    lstAllClasses.addAll(getReversePostOrderListIterative(rootClasses));

    // rename all interfaces and classes
    for (ClassWrapperNode node : lstAllClasses) {
      renameClass(node.getClassStruct());
    }
  }

  private void renameClass(StructClass cl) {

    if (!cl.isOwn()) {
      return;
    }

    String classOldFullName = cl.qualifiedName;

    // TODO: rename packages
    String clSimpleName = ConverterHelper.getSimpleClassName(classOldFullName);
    if (helper.toBeRenamed(IIdentifierRenamer.Type.ELEMENT_CLASS, clSimpleName, null, null)) {
      String classNewFullName;

      do {
        String classname = helper.getNextClassName(classOldFullName, ConverterHelper.getSimpleClassName(classOldFullName));
        classNewFullName = ConverterHelper.replaceSimpleClassName(classOldFullName, classname);
      }
      while (context.hasClass(classNewFullName));

      interceptor.addName(classOldFullName, classNewFullName);
    }
  }

  private void renameClassIdentifiers(StructClass cl, Map<String, String> names) {
    // all classes are already renamed
    String classOldFullName = cl.qualifiedName;
    String classNewFullName = interceptor.getName(classOldFullName);

    if (classNewFullName == null) {
      classNewFullName = classOldFullName;
    }

    // methods
    HashSet<String> setMethodNames = new HashSet<>();
    for (StructMethod md : cl.getMethods()) {
      setMethodNames.add(md.getName());
    }

    VBStyleCollection<StructMethod, String> methods = cl.getMethods();
    for (int i = 0; i < methods.size(); i++) {

      StructMethod mt = methods.get(i);
      String key = methods.getKey(i);

      boolean isPrivate = mt.hasModifier(CodeConstants.ACC_PRIVATE);

      String name = mt.getName();
      if (!cl.isOwn() || mt.hasModifier(CodeConstants.ACC_NATIVE)) {
        // external and native methods must not be renamed
        if (!isPrivate) {
          names.put(key, name);
        }
      }
      else if (helper.toBeRenamed(IIdentifierRenamer.Type.ELEMENT_METHOD, classOldFullName, name, mt.getDescriptor())) {
        if (isPrivate || !names.containsKey(key)) {
          do {
            name = helper.getNextMethodName(classOldFullName, name, mt.getDescriptor());
          }
          while (setMethodNames.contains(name));

          if (!isPrivate) {
            names.put(key, name);
          }
        }
        else {
          name = names.get(key);
        }

        interceptor.addName(classOldFullName + " " + mt.getName() + " " + mt.getDescriptor(),
                            classNewFullName + " " + name + " " + buildNewDescriptor(false, mt.getDescriptor()));
      }
    }

    // external fields are not being renamed
    if (!cl.isOwn()) {
      return;
    }

    // fields
    // FIXME: should overloaded fields become the same name?
    HashSet<String> setFieldNames = new HashSet<>();
    for (StructField fd : cl.getFields()) {
      setFieldNames.add(fd.getName());
    }

    for (StructField fd : cl.getFields()) {
      if (helper.toBeRenamed(IIdentifierRenamer.Type.ELEMENT_FIELD, classOldFullName, fd.getName(), fd.getDescriptor())) {
        String newName;
        do {
          newName = helper.getNextFieldName(classOldFullName, fd.getName(), fd.getDescriptor());
        }
        while (setFieldNames.contains(newName));

        interceptor.addName(classOldFullName + " " + fd.getName() + " " + fd.getDescriptor(),
                            classNewFullName + " " + newName + " " + buildNewDescriptor(true, fd.getDescriptor()));
      }
    }
  }

  @Override
  public String buildNewClassname(String className) {
    return interceptor.getName(className);
  }

  private String buildNewDescriptor(boolean isField, String descriptor) {
    String newDescriptor;
    if (isField) {
      newDescriptor = FieldDescriptor.parseDescriptor(descriptor).buildNewDescriptor(this);
    }
    else {
      newDescriptor = MethodDescriptor.parseDescriptor(descriptor).buildNewDescriptor(this);
    }
    return newDescriptor != null ? newDescriptor : descriptor;
  }

  private static List<ClassWrapperNode> getReversePostOrderListIterative(List<ClassWrapperNode> roots) {
    List<ClassWrapperNode> res = new ArrayList<>();

    LinkedList<ClassWrapperNode> stackNode = new LinkedList<>();
    LinkedList<Integer> stackIndex = new LinkedList<>();

    Set<ClassWrapperNode> setVisited = new HashSet<>();

    for (ClassWrapperNode root : roots) {
      stackNode.add(root);
      stackIndex.add(0);
    }

    while (!stackNode.isEmpty()) {
      ClassWrapperNode node = stackNode.getLast();
      int index = stackIndex.removeLast();

      setVisited.add(node);

      List<ClassWrapperNode> lstSubs = node.getSubclasses();

      for (; index < lstSubs.size(); index++) {
        ClassWrapperNode sub = lstSubs.get(index);
        if (!setVisited.contains(sub)) {
          stackIndex.add(index + 1);
          stackNode.add(sub);
          stackIndex.add(0);
          break;
        }
      }

      if (index == lstSubs.size()) {
        res.add(0, node);
        stackNode.removeLast();
      }
    }

    return res;
  }

  private void buildInheritanceTree() {
    Map<String, ClassWrapperNode> nodes = new LinkedHashMap<>();
    List<StructClass> classes = context.getOwnClasses();

    List<ClassWrapperNode> rootClasses = new ArrayList<>();
    List<ClassWrapperNode> rootInterfaces = new ArrayList<>();

    for (StructClass cl : classes) {
      LinkedList<StructClass> stack = new LinkedList<>();
      LinkedList<ClassWrapperNode> stackSubNodes = new LinkedList<>();

      stack.add(cl);
      stackSubNodes.add(null);

      while (!stack.isEmpty()) {
        StructClass clStr = stack.removeFirst();
        ClassWrapperNode child = stackSubNodes.removeFirst();

        ClassWrapperNode node = nodes.get(clStr.qualifiedName);
        boolean isNewNode = (node == null);

        if (isNewNode) {
          nodes.put(clStr.qualifiedName, node = new ClassWrapperNode(clStr));
        }

        if (child != null) {
          node.addSubclass(child);
        }

        if (!isNewNode) {
          break;
        }
        else {
          boolean isInterface = clStr.hasModifier(CodeConstants.ACC_INTERFACE);
          boolean found_parent = false;

          if (isInterface) {
            for (String ifName : clStr.getInterfaceNames()) {
              StructClass clParent = context.getClass(ifName);
              if (clParent != null) {
                stack.add(clParent);
                stackSubNodes.add(node);
                found_parent = true;
              }
            }
          }
          else if (clStr.superClass != null) { // null iff java/lang/Object
            StructClass clParent = context.getClass(clStr.superClass.getString());
            if (clParent != null) {
              stack.add(clParent);
              stackSubNodes.add(node);
              found_parent = true;
            }
          }

          if (!found_parent) { // no super class or interface
            (isInterface ? rootInterfaces : rootClasses).add(node);
          }
        }
      }
    }

    this.rootClasses = rootClasses;
    this.rootInterfaces = rootInterfaces;
  }
}