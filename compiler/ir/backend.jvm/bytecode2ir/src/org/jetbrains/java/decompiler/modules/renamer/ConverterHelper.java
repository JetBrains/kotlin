// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.renamer;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.extern.IIdentifierRenamer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class ConverterHelper implements IIdentifierRenamer {
  private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
    "abstract", "do", "if", "package", "synchronized", "boolean", "double", "implements", "private", "this", "break", "else", "import",
    "protected", "throw", "byte", "extends", "instanceof", "public", "throws", "case", "false", "int", "return", "transient", "catch",
    "final", "interface", "short", "true", "char", "finally", "long", "static", "try", "class", "float", "native", "strictfp", "void",
    "const", "for", "new", "super", "volatile", "continue", "goto", "null", "switch", "while", "default", "assert", "enum"));
  private static final Set<String> RESERVED_WINDOWS_NAMESPACE = new HashSet<>(Arrays.asList(
    "con", "prn", "aux", "nul",
    "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
    "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9"));

  private int classCounter = 0;
  private int fieldCounter = 0;
  private int methodCounter = 0;
  private static final Pattern OBF_REGEX = Pattern.compile("^(?:.{1,3}(?:$|\\$))+");
  private final Set<String> setNonStandardClassNames = new HashSet<>();

  @Override
  public boolean toBeRenamed(Type elementType, String className, String element, String descriptor) {
    String value = elementType == Type.ELEMENT_CLASS ? className : element;
    boolean isWindowsReserved;
    return value == null || value.isEmpty() || !isValidIdentifier(elementType == Type.ELEMENT_METHOD, value) || KEYWORDS.contains(value) ||
        (!(isWindowsReserved = RESERVED_WINDOWS_NAMESPACE.contains(value.toLowerCase(Locale.ENGLISH))) && OBF_REGEX.matcher(value).matches()) ||
        (elementType == Type.ELEMENT_CLASS && (isWindowsReserved || value.length() > 255 - 6)); // account for .class
  }

  /**
   * Return {@code true} if, and only if identifier passed is compliant to JLS9 section 3.8 AND DOES NOT CONTAINS so-called "ignorable" characters.
   * Ignorable characters are removed by javac silently during compilation and thus may appear only in specially crafted obfuscated classes.
   * For more information about "ignorable" characters see <a href="https://bugs.openjdk.java.net/browse/JDK-7144981">JDK-7144981</a>.
   *
   * @param identifier Identifier to be checked
   * @return {@code true} in case {@code identifier} passed can be used as an identifier; {@code false} otherwise.
   */
  private static boolean isValidIdentifier(boolean isMethod, String identifier) {

    assert identifier != null : "Null identifier passed to the isValidIdentifier() method.";
    assert !identifier.isEmpty() : "Empty identifier passed to the isValidIdentifier() method.";

    if (isMethod && (identifier.equals(CodeConstants.INIT_NAME) || identifier.equals(CodeConstants.CLINIT_NAME))) {
      return true;
    }

    if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
      return false;
    }

    char[] chars = identifier.toCharArray();

    for(int i = 1; i < chars.length; i++) {
      char ch = chars[i];

      if ((!Character.isJavaIdentifierPart(ch)) || Character.isIdentifierIgnorable(ch)) {
        return false;
      }
    }

    return true;

  }

  // TODO: consider possible conflicts with not renamed classes, fields and methods!
  // We should get all relevant information here.
  @Override
  public String getNextClassName(String fullName, String shortName) {
    if (shortName == null) {
      return "class_" + (classCounter++);
    }

    int index = 0;
    while (index < shortName.length() && Character.isDigit(shortName.charAt(index))) {
      index++;
    }

    if (index == 0 || index == shortName.length()) {
      return "class_" + (classCounter++);
    }
    else {
      String name = shortName.substring(index);
      if (setNonStandardClassNames.contains(name)) {
        return "Inner" + name + "_" + (classCounter++);
      }
      else {
        setNonStandardClassNames.add(name);
        return "Inner" + name;
      }
    }
  }

  @Override
  public String getNextFieldName(String className, String field, String descriptor) {
    return "field_" + (fieldCounter++);
  }

  @Override
  public String getNextMethodName(String className, String method, String descriptor) {
    return "method_" + (methodCounter++);
  }

  // *****************************************************************************
  // static methods
  // *****************************************************************************

  public static String getSimpleClassName(String fullName) {
    return fullName.substring(fullName.lastIndexOf('/') + 1);
  }

  public static String replaceSimpleClassName(String fullName, String newName) {
    return fullName.substring(0, fullName.lastIndexOf('/') + 1) + newName;
  }
}