package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.modules.decompiler.exps.ConstExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.FieldExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.attr.StructBootstrapMethodsAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.consts.LinkConstant;
import org.jetbrains.java.decompiler.struct.consts.PooledConstant;
import org.jetbrains.java.decompiler.struct.consts.PrimitiveConstant;
import org.jetbrains.java.decompiler.struct.gen.FieldDescriptor;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.struct.gen.VarType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Handles the java.lang.invoke.ConstantBootstraps bootstraps
public class CondyHelper {

  // TODO: handle other bootstraps (invoke, explicitCast)
  private static final String CONSTANT_BOOTSTRAPS_CLASS = "java/lang/invoke/ConstantBootstraps";

  // converts a condy exprent into an equivalent "normal java" exprent
  public static Exprent simplifyCondy(InvocationExprent condyExpr) {
    if (condyExpr.getInvocationType() != InvocationExprent.InvocationType.CONSTANT_DYNAMIC) {
      return condyExpr;
    }

    LinkConstant method = condyExpr.getBootstrapMethod();
    if (!CONSTANT_BOOTSTRAPS_CLASS.equals(method.classname)) {
      return condyExpr;
    }

    switch (method.elementname) {
      case "nullConstant": // -> null
        // TODO: include target type?
        return new ConstExprent(VarType.VARTYPE_NULL, null, null).setWasCondy(true);
      case "primitiveClass": // -> int.class
        String desc = condyExpr.getName();
        // the name of the constant is the descriptor of the primitive type, check that its valid
        if (desc.length() != 1 || !("ZCBSIJFDV".contains(desc))) {
          break;
        }
        VarType type = new VarType(desc, false);
        return new ConstExprent(VarType.VARTYPE_CLASS, ExprProcessor.getCastTypeName(type), null).setWasCondy(true);
      case "enumConstant": // MyEnum.NAME
        String typeName = condyExpr.getExprType().value;
        return new FieldExprent(condyExpr.getName(), typeName, true, null, FieldDescriptor.parseDescriptor("L" + typeName + ";"), null, false, true);
      case "getStaticFinal": { // MyClass.fieldName
        // name of the constant is the field name
        List<PooledConstant> constArgs = condyExpr.getBootstrapArguments();
        String fieldType = condyExpr.getExprType().value;
        String ownerClass;
        // if a constant argument is present, that argument must be a class that contains the field
        if (constArgs.size() == 1) {
          PooledConstant ownerName = constArgs.get(0);
          if (ownerName instanceof PrimitiveConstant) {
            ownerClass = ((PrimitiveConstant) ownerName).value.toString();
          } else {
            return condyExpr;
          }
        // otherwise, the field is declared in the type of the field
        } else {
          if (condyExpr.getExprType().type != VarType.VARTYPE_OBJECT.type) {
            return condyExpr;
          }
          ownerClass = fieldType;
        }
        return new FieldExprent(condyExpr.getName(), ownerClass, true, null, FieldDescriptor.parseDescriptor(fieldType), null, false, true);
      }
      case "fieldVarHandle":
      case "staticFieldVarHandle": { // --> MethodHandles.lookup().find[Static]VarHandle(...)
        if (!DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_COMPLEX_CONDYS)) {
          return condyExpr;
        }
        boolean isStatic = method.elementname.startsWith("static");
        List<PooledConstant> constArgs = condyExpr.getBootstrapArguments();
        String fieldName = condyExpr.getName();
        // first argument is fieldname so should be primitive, second might be condy for primitive classes
        if (constArgs.size() != 2 || !(constArgs.get(0) instanceof PrimitiveConstant)) {
          return condyExpr;
        }
        String ownerClass = ((PrimitiveConstant) constArgs.get(0)).getString();
        return constructVarHandle(fieldName, ownerClass, constArgs.get(1), isStatic);
      }
      case "arrayVarHandle": { // --> MethodHandles.arrayElementVarHandle(...)
        if (!DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_COMPLEX_CONDYS)) {
          return condyExpr;
        }
        // argument is the array class
        List<PooledConstant> constArgs = condyExpr.getBootstrapArguments();
        if (constArgs.size() != 1) {
          return condyExpr;
        }
        return constructArrayVarHandleExprent(constArgs.get(0));
      }
    }
    return condyExpr;
  }

  private static Exprent constructVarHandle(String fieldName, String fieldOwner, PooledConstant fieldType, boolean isStatic) {
    // makes an invocation exprent for MethodHandles.lookup().find[Static]VarHandle(fieldOwner.class, fieldName, fieldType.class)
    Exprent lookupExprent = constructLookupExprent();
    VarType ownerClassClass = new VarType(fieldOwner, false);
    Exprent ownerClassConst = new ConstExprent(VarType.VARTYPE_CLASS, ExprProcessor.getCastTypeName(ownerClassClass), null);
    Exprent fieldNameConst = new ConstExprent(VarType.VARTYPE_STRING, fieldName, null);
    Exprent fieldTypeConst = toClassExprent(fieldType);
    return constructFindVarHandleExprent(isStatic, lookupExprent, ownerClassConst, fieldNameConst, fieldTypeConst);
  }

  private static Exprent toClassExprent(PooledConstant constant) {
    // if the constant is a primitive constant (assumed a class), makes a class constant
    Exprent constExpr;
    if (constant instanceof PrimitiveConstant) {
      VarType fieldTypeClass = new VarType(((PrimitiveConstant) constant).getString(), false);
      constExpr = new ConstExprent(VarType.VARTYPE_CLASS, ExprProcessor.getCastTypeName(fieldTypeClass), null);
    } else {
      // otherwise we have a condy, expand and simplify it like normal
      // assume it has the correct type
      constExpr = toCondyExprent((LinkConstant) constant);
    }
    return constExpr;
  }

  private static Exprent toCondyExprent(LinkConstant fieldType) {
    Exprent fieldTypeConst;
    // TODO: is this correct in non-trivial cases?
    StructClass cl = (StructClass) DecompilerContext.getProperty(DecompilerContext.CURRENT_CLASS);
    // same as in ExprProcessor, use bootstrap attribute from current file to link the constant to bootstrap method
    StructBootstrapMethodsAttribute bootstrap = cl.getAttribute(StructGeneralAttribute.ATTRIBUTE_BOOTSTRAP_METHODS);
    LinkConstant bootstrapMethod = null;
    List<PooledConstant> constArgs = null;
    if (bootstrap != null) {
      bootstrapMethod = bootstrap.getMethodReference(fieldType.index1);
      constArgs = bootstrap.getMethodArguments(fieldType.index1);
    }
    InvocationExprent arg = new InvocationExprent(CodeConstants.opc_ldc, fieldType, bootstrapMethod, constArgs, null, null);
    // simplify nested condys, for e.g. fieldVarHandle(...int.class)
    fieldTypeConst = simplifyCondy(arg);
    if (fieldTypeConst instanceof ConstExprent) {
      ((ConstExprent) fieldTypeConst).setWasCondy(false); // comment is redundant for nested condys
    }
    return fieldTypeConst;
  }

  private static InvocationExprent constructLookupExprent() {
    // creates an exprent for MethodHandles.lookup()
    InvocationExprent exprent = new InvocationExprent();
    exprent.setName("lookup");
    exprent.setClassname("java/lang/invoke/MethodHandles");
    String desc = "()Ljava/lang/invoke/MethodHandles$Lookup;";
    exprent.setStringDescriptor(desc);
    exprent.setDescriptor(MethodDescriptor.parseDescriptor(desc));
    exprent.setFunctype(InvocationExprent.Type.GENERAL);
    exprent.setStatic(true);
    return exprent;
  }

  private static InvocationExprent constructFindVarHandleExprent(boolean isStatic, Exprent lookup, Exprent ownerClass, Exprent fieldName, Exprent fieldClass) {
    // creates an exprent for [lookup()].find[Static]VarHandle(Owner.class, "name", Type.class)
    // the receiver is passed in, should be made in `constructLookupExprent`
    InvocationExprent exprent = new InvocationExprent();
    exprent.setName(isStatic ? "findStaticVarHandle" : "findVarHandle");
    exprent.setClassname("java/lang/invoke/MethodHandles$Lookup");
    String desc = "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;";
    exprent.setStringDescriptor(desc);
    exprent.setDescriptor(MethodDescriptor.parseDescriptor(desc));
    exprent.setFunctype(InvocationExprent.Type.GENERAL);
    exprent.setStatic(false);
    exprent.setInstance(lookup);
    exprent.setLstParameters(Arrays.asList(ownerClass, fieldName, fieldClass));
    return exprent.markWasLazyCondy();
  }

  private static InvocationExprent constructArrayVarHandleExprent(PooledConstant classConst) {
    // creates an exprent for MethodHandles.arrayElementVarHandle(Array[].class)
    InvocationExprent exprent = new InvocationExprent();
    exprent.setName("arrayElementVarHandle");
    exprent.setClassname("java/lang/invoke/MethodHandles");
    String desc = "(Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;";
    exprent.setStringDescriptor(desc);
    exprent.setDescriptor(MethodDescriptor.parseDescriptor(desc));
    exprent.setFunctype(InvocationExprent.Type.GENERAL);
    exprent.setStatic(true);
    Exprent classExpr = toClassExprent(classConst);
    exprent.setLstParameters(Collections.singletonList(classExpr));
    return exprent.markWasLazyCondy();
  }
}
