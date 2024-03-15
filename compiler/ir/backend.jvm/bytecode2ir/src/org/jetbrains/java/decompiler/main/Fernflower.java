// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main;

import org.jetbrains.java.decompiler.main.ClassesProcessor.ClassNode;
import org.jetbrains.java.decompiler.main.extern.*;
import org.jetbrains.java.decompiler.modules.renamer.ConverterHelper;
import org.jetbrains.java.decompiler.modules.renamer.IdentifierConverter;
import org.jetbrains.java.decompiler.modules.renamer.PoolInterceptor;
import org.jetbrains.java.decompiler.struct.IDecompiledData;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructContext;
import org.jetbrains.java.decompiler.struct.lazy.LazyLoader;
import org.jetbrains.java.decompiler.util.ClasspathScanner;
import org.jetbrains.java.decompiler.util.JADNameProvider;
import org.jetbrains.java.decompiler.util.JrtFinder;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Fernflower implements IDecompiledData {
  private final StructContext structContext;
  private final ClassesProcessor classProcessor;
  private final IIdentifierRenamer helper;
  private final IdentifierConverter converter;

  public Fernflower(IResultSaver saver, Map<String, Object> customProperties, IFernflowerLogger logger) {
    this(null, saver, customProperties, logger);
  }

  @Deprecated
  public Fernflower(IBytecodeProvider provider, IResultSaver saver, Map<String, Object> customProperties, IFernflowerLogger logger) {
    Map<String, Object> properties = new HashMap<>(IFernflowerPreferences.DEFAULTS);
    if (customProperties != null) {
      properties.putAll(customProperties);
    }

    String level = (String)properties.get(IFernflowerPreferences.LOG_LEVEL);
    if (level != null) {
      try {
        logger.setSeverity(IFernflowerLogger.Severity.valueOf(level.toUpperCase(Locale.ENGLISH)));
      }
      catch (IllegalArgumentException ignore) { }
    }

    structContext = new StructContext(provider, saver, this);
    classProcessor = new ClassesProcessor(structContext);

    PoolInterceptor interceptor = null;
    if ("1".equals(properties.get(IFernflowerPreferences.RENAME_ENTITIES))) {
      helper = loadHelper((String)properties.get(IFernflowerPreferences.USER_RENAMER_CLASS), logger);
      interceptor = new PoolInterceptor();
      converter = new IdentifierConverter(structContext, helper, interceptor);
    }
    else {
      helper = null;
      converter = null;
    }

    IVariableNamingFactory renamerFactory = null;
    String factoryClazz = (String) properties.get(DecompilerContext.RENAMER_FACTORY);
    if (factoryClazz != null) {
      try {
        renamerFactory = Class.forName(factoryClazz).asSubclass(IVariableNamingFactory.class).getDeclaredConstructor().newInstance();
      } catch (Exception e) {
        logger.writeMessage("Error loading renamer factory class: " + factoryClazz, e);
      }
    }
    if (renamerFactory == null) {
      if("1".equals(properties.get(IFernflowerPreferences.USE_JAD_VARNAMING))) {
        boolean renameParams = "1".equals(properties.get(IFernflowerPreferences.USE_JAD_PARAMETER_NAMING));
        renamerFactory = new JADNameProvider.JADNameProviderFactory(renameParams);
      } else {
        renamerFactory = new IdentityRenamerFactory();
      }
    }

    DecompilerContext context = new DecompilerContext(properties, logger, structContext, classProcessor, interceptor, renamerFactory);
    DecompilerContext.setCurrentContext(context);

    String vendor = System.getProperty("java.vendor", "missing vendor");
    String javaVersion = System.getProperty("java.version", "missing java version");
    String jvmVersion = System.getProperty("java.vm.version", "missing jvm version");
    logger.writeMessage(String.format("JVM info: %s - %s - %s", vendor, javaVersion, jvmVersion), IFernflowerLogger.Severity.INFO);

    if (DecompilerContext.getOption(IFernflowerPreferences.INCLUDE_ENTIRE_CLASSPATH)) {
      ClasspathScanner.addAllClasspath(structContext);
    } else if (!DecompilerContext.getProperty(IFernflowerPreferences.INCLUDE_JAVA_RUNTIME).toString().isEmpty()) {
      final String javaRuntime = DecompilerContext.getProperty(IFernflowerPreferences.INCLUDE_JAVA_RUNTIME).toString();
      if (javaRuntime.equalsIgnoreCase(JrtFinder.CURRENT) || javaRuntime.equalsIgnoreCase("1")) {
        JrtFinder.addRuntime(structContext);
      } else if (!javaRuntime.equalsIgnoreCase("0")) {
        JrtFinder.addRuntime(structContext, new File(javaRuntime));
      }
    }
  }

  private static IIdentifierRenamer loadHelper(String className, IFernflowerLogger logger) {
    if (className != null) {
      try {
        Class<?> renamerClass = Fernflower.class.getClassLoader().loadClass(className);
        return (IIdentifierRenamer) renamerClass.getDeclaredConstructor().newInstance();
      }
      catch (Exception e) {
        logger.writeMessage("Cannot load renamer '" + className + "'", IFernflowerLogger.Severity.WARN, e);
      }
    }

    return new ConverterHelper();
  }

  public void addSource(IContextSource source) {
    structContext.addSpace(source, true);
  }

  public void addSource(File source) {
    structContext.addSpace(source, true);
  }

  public void addLibrary(IContextSource library) {
    structContext.addSpace(library, false);
  }

  public void addLibrary(File library) {
    structContext.addSpace(library, false);
  }

  public void decompileContext() {
    if (converter != null) {
      converter.rename();
    }

    classProcessor.loadClasses(helper);

    structContext.saveContext();
  }

  public void addWhitelist(String prefix) {
    classProcessor.addWhitelist(prefix);
  }

  public void clearContext() {
    structContext.clear();
    DecompilerContext.setCurrentContext(null);
  }

  @Override
  public String getClassEntryName(StructClass cl, String entryName) {
    ClassNode node = classProcessor.getMapRootClasses().get(cl.qualifiedName);
    if (node == null || node.type != ClassNode.Type.ROOT) {
      return null;
    }
    else if (converter != null) {
      String simpleClassName = cl.qualifiedName.substring(cl.qualifiedName.lastIndexOf('/') + 1);
      return entryName.substring(0, entryName.lastIndexOf('/') + 1) + simpleClassName + ".java";
    }
    else {
      final int clazzIdx = entryName.lastIndexOf(".class");
      if (clazzIdx == -1) {
        return entryName + ".java";
      } else {
        return entryName.substring(0, clazzIdx) + ".java";
      }
    }
  }

  @Override
  public void processClass(final StructClass cl) throws IOException {
      classProcessor.processClass(cl); // unhandled exceptions handled later on
  }

  @Override
  public String getClassContent(StructClass cl) {
    TextBuffer buffer = new TextBuffer(ClassesProcessor.AVERAGE_CLASS_SIZE);
    try {
      buffer.append(DecompilerContext.getProperty(IFernflowerPreferences.BANNER).toString());
      classProcessor.writeClass(cl, buffer);
      String res = buffer.convertToStringAndAllowDataDiscard();
      if (res == null) {
        return "$ VF: Unable to decompile class " + cl.qualifiedName;
      }

      return res;
    }
    catch (Throwable t) {
      DecompilerContext.getLogger().writeMessage("Class " + cl.qualifiedName + " couldn't be fully decompiled.", t);
      if (DecompilerContext.getOption(IFernflowerPreferences.DUMP_EXCEPTION_ON_ERROR)) {
        List<String> lines = new ArrayList<>();
        lines.add("/*");
        lines.add("$VF: Unable to decompile class");
        lines.addAll(ClassWriter.getErrorComment());
        ClassWriter.collectErrorLines(t, lines);
        lines.add("*/");
        return String.join(DecompilerContext.getNewLineSeparator(), lines);
      } else {
        return null;
      }
    }
  }
}