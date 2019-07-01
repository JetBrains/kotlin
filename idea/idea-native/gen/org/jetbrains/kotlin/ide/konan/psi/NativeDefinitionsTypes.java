// This is a generated file. Not intended for manual editing.
package org.jetbrains.kotlin.ide.konan.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;

public interface NativeDefinitionsTypes {

  IElementType BOOL_KEY = new NativeDefinitionsElementType("BOOL_KEY");
  IElementType BOOL_VALUE = new NativeDefinitionsElementType("BOOL_VALUE");
  IElementType CODE = new NativeDefinitionsElementType("CODE");
  IElementType STRINGS_KEY = new NativeDefinitionsElementType("STRINGS_KEY");
  IElementType STRINGS_VALUE = new NativeDefinitionsElementType("STRINGS_VALUE");
  IElementType STRING_KEY = new NativeDefinitionsElementType("STRING_KEY");
  IElementType STRING_VALUE = new NativeDefinitionsElementType("STRING_VALUE");

  IElementType ANDROID = new NativeDefinitionsTokenType("android");
  IElementType ANDROID_ARM32 = new NativeDefinitionsTokenType("android_arm32");
  IElementType ANDROID_ARM64 = new NativeDefinitionsTokenType("android_arm64");
  IElementType ARM32 = new NativeDefinitionsTokenType("arm32");
  IElementType ARM64 = new NativeDefinitionsTokenType("arm64");
  IElementType CODE_CHARS = new NativeDefinitionsTokenType("CODE_CHARS");
  IElementType COMMENT = new NativeDefinitionsTokenType("COMMENT");
  IElementType COMPILER_OPTS = new NativeDefinitionsTokenType("compilerOpts");
  IElementType DELIM = new NativeDefinitionsTokenType("DELIM");
  IElementType DEPENDS = new NativeDefinitionsTokenType("depends");
  IElementType DISABLE_DESIGNATED_INITIALIZER_CHECKS = new NativeDefinitionsTokenType("disableDesignatedInitializerChecks");
  IElementType ENTRY_POINT = new NativeDefinitionsTokenType("entryPoint");
  IElementType EXCLUDED_FUNCTIONS = new NativeDefinitionsTokenType("excludedFunctions");
  IElementType EXCLUDED_MACROS = new NativeDefinitionsTokenType("excludedMacros");
  IElementType EXCLUDE_DEPENDENT_MODULES = new NativeDefinitionsTokenType("excludeDependentModules");
  IElementType EXCLUDE_SYSTEM_LIBS = new NativeDefinitionsTokenType("excludeSystemLibs");
  IElementType EXPORT_FORWARD_DECLARATIONS = new NativeDefinitionsTokenType("exportForwardDeclarations");
  IElementType HEADERS = new NativeDefinitionsTokenType("headers");
  IElementType HEADER_FILTER = new NativeDefinitionsTokenType("headerFilter");
  IElementType IOS = new NativeDefinitionsTokenType("ios");
  IElementType IOS_ARM32 = new NativeDefinitionsTokenType("ios_arm32");
  IElementType IOS_ARM64 = new NativeDefinitionsTokenType("ios_arm64");
  IElementType IOS_X64 = new NativeDefinitionsTokenType("ios_x64");
  IElementType LANGUAGE = new NativeDefinitionsTokenType("language");
  IElementType LIBRARY_PATHS = new NativeDefinitionsTokenType("libraryPaths");
  IElementType LINKER = new NativeDefinitionsTokenType("linker");
  IElementType LINKER_OPTS = new NativeDefinitionsTokenType("linkerOpts");
  IElementType LINUX = new NativeDefinitionsTokenType("linux");
  IElementType LINUX_ARM32_HFP = new NativeDefinitionsTokenType("linux_arm32_hfp");
  IElementType LINUX_MIPS32 = new NativeDefinitionsTokenType("linux_mips32");
  IElementType LINUX_MIPSEL32 = new NativeDefinitionsTokenType("linux_mipsel32");
  IElementType LINUX_X64 = new NativeDefinitionsTokenType("linux_x64");
  IElementType MACOS_X64 = new NativeDefinitionsTokenType("macos_x64");
  IElementType MINGW = new NativeDefinitionsTokenType("mingw");
  IElementType MINGW_X64 = new NativeDefinitionsTokenType("mingw_x64");
  IElementType MIPS32 = new NativeDefinitionsTokenType("mips32");
  IElementType MIPSEL32 = new NativeDefinitionsTokenType("mipsel32");
  IElementType MODULES = new NativeDefinitionsTokenType("modules");
  IElementType NON_STRICT_ENUMS = new NativeDefinitionsTokenType("nonStrictEnums");
  IElementType NO_STRING_CONVERSION = new NativeDefinitionsTokenType("noStringConversion");
  IElementType OSX = new NativeDefinitionsTokenType("osx");
  IElementType PACKAGE = new NativeDefinitionsTokenType("package");
  IElementType SEPARATOR = new NativeDefinitionsTokenType("SEPARATOR");
  IElementType STATIC_LIBRARIES = new NativeDefinitionsTokenType("staticLibraries");
  IElementType STRICT_ENUMS = new NativeDefinitionsTokenType("strictEnums");
  IElementType UNKNOWN_KEY = new NativeDefinitionsTokenType("<unknown key>");
  IElementType UNKNOWN_PLATFORM = new NativeDefinitionsTokenType("<unknown platform>");
  IElementType VALUE = new NativeDefinitionsTokenType("VALUE");
  IElementType WASM = new NativeDefinitionsTokenType("wasm");
  IElementType WASM32 = new NativeDefinitionsTokenType("wasm32");
  IElementType X64 = new NativeDefinitionsTokenType("x64");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == BOOL_KEY) {
        return new NativeDefinitionsBoolKeyImpl(node);
      }
      else if (type == BOOL_VALUE) {
        return new NativeDefinitionsBoolValueImpl(node);
      }
      else if (type == CODE) {
        return new NativeDefinitionsCodeImpl(node);
      }
      else if (type == STRINGS_KEY) {
        return new NativeDefinitionsStringsKeyImpl(node);
      }
      else if (type == STRINGS_VALUE) {
        return new NativeDefinitionsStringsValueImpl(node);
      }
      else if (type == STRING_KEY) {
        return new NativeDefinitionsStringKeyImpl(node);
      }
      else if (type == STRING_VALUE) {
        return new NativeDefinitionsStringValueImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
