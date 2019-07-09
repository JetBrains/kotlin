// This is a generated file. Not intended for manual editing.
package org.jetbrains.kotlin.ide.konan;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static org.jetbrains.kotlin.ide.konan.psi.NativeDefinitionsTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class NativeDefinitionsParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    if (t == BOOL_KEY) {
      r = boolKey(b, 0);
    }
    else if (t == BOOL_VALUE) {
      r = boolValue(b, 0);
    }
    else if (t == CODE) {
      r = code(b, 0);
    }
    else if (t == STRING_KEY) {
      r = stringKey(b, 0);
    }
    else if (t == STRING_VALUE) {
      r = stringValue(b, 0);
    }
    else if (t == STRINGS_KEY) {
      r = stringsKey(b, 0);
    }
    else if (t == STRINGS_VALUE) {
      r = stringsValue(b, 0);
    }
    else {
      r = parse_root_(t, b, 0);
    }
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return root(b, l + 1);
  }

  /* ********************************************************** */
  // boolKey platform_? SEPARATOR boolValue
  static boolean boolDefinition_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "boolDefinition_")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = boolKey(b, l + 1);
    r = r && boolDefinition__1(b, l + 1);
    r = r && consumeToken(b, SEPARATOR);
    r = r && boolValue(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // platform_?
  private static boolean boolDefinition__1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "boolDefinition__1")) return false;
    platform_(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // DISABLE_DESIGNATED_INITIALIZER_CHECKS | EXCLUDE_DEPENDENT_MODULES | EXCLUDE_SYSTEM_LIBS
  public static boolean boolKey(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "boolKey")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, BOOL_KEY, "<bool key>");
    r = consumeToken(b, DISABLE_DESIGNATED_INITIALIZER_CHECKS);
    if (!r) r = consumeToken(b, EXCLUDE_DEPENDENT_MODULES);
    if (!r) r = consumeToken(b, EXCLUDE_SYSTEM_LIBS);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // VALUE
  public static boolean boolValue(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "boolValue")) return false;
    if (!nextTokenIs(b, VALUE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, VALUE);
    exit_section_(b, m, BOOL_VALUE, r);
    return r;
  }

  /* ********************************************************** */
  // CODE_CHARS
  public static boolean code(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "code")) return false;
    if (!nextTokenIs(b, CODE_CHARS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CODE_CHARS);
    exit_section_(b, m, CODE, r);
    return r;
  }

  /* ********************************************************** */
  // COMMENT | boolDefinition_ | stringDefinition_ | stringsDefinition_ | incorrectDefinition_
  static boolean definitionItem_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "definitionItem_")) return false;
    boolean r;
    r = consumeToken(b, COMMENT);
    if (!r) r = boolDefinition_(b, l + 1);
    if (!r) r = stringDefinition_(b, l + 1);
    if (!r) r = stringsDefinition_(b, l + 1);
    if (!r) r = incorrectDefinition_(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // LANGUAGE | LINKER
  static boolean extensibleStringKey_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "extensibleStringKey_")) return false;
    if (!nextTokenIs(b, "", LANGUAGE, LINKER)) return false;
    boolean r;
    r = consumeToken(b, LANGUAGE);
    if (!r) r = consumeToken(b, LINKER);
    return r;
  }

  /* ********************************************************** */
  // UNKNOWN_KEY? UNKNOWN_PLATFORM? SEPARATOR VALUE? | UNKNOWN_KEY UNKNOWN_PLATFORM?
  static boolean incorrectDefinition_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "incorrectDefinition_")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = incorrectDefinition__0(b, l + 1);
    if (!r) r = incorrectDefinition__1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // UNKNOWN_KEY? UNKNOWN_PLATFORM? SEPARATOR VALUE?
  private static boolean incorrectDefinition__0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "incorrectDefinition__0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = incorrectDefinition__0_0(b, l + 1);
    r = r && incorrectDefinition__0_1(b, l + 1);
    r = r && consumeToken(b, SEPARATOR);
    r = r && incorrectDefinition__0_3(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // UNKNOWN_KEY?
  private static boolean incorrectDefinition__0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "incorrectDefinition__0_0")) return false;
    consumeToken(b, UNKNOWN_KEY);
    return true;
  }

  // UNKNOWN_PLATFORM?
  private static boolean incorrectDefinition__0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "incorrectDefinition__0_1")) return false;
    consumeToken(b, UNKNOWN_PLATFORM);
    return true;
  }

  // VALUE?
  private static boolean incorrectDefinition__0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "incorrectDefinition__0_3")) return false;
    consumeToken(b, VALUE);
    return true;
  }

  // UNKNOWN_KEY UNKNOWN_PLATFORM?
  private static boolean incorrectDefinition__1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "incorrectDefinition__1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, UNKNOWN_KEY);
    r = r && incorrectDefinition__1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // UNKNOWN_PLATFORM?
  private static boolean incorrectDefinition__1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "incorrectDefinition__1_1")) return false;
    consumeToken(b, UNKNOWN_PLATFORM);
    return true;
  }

  /* ********************************************************** */
  // ANDROID | ANDROID_ARM32 | ANDROID_ARM64 | ARM32 | ARM64 | IOS | IOS_ARM32
  //                         | IOS_ARM64 | IOS_X64 | LINUX | LINUX_ARM32_HFP | LINUX_MIPS32
  //                         | LINUX_MIPSEL32 | LINUX_X64 | MACOS_X64 | MINGW | MINGW_X64 | MIPS32
  //                         | MIPSEL32 | OSX | WASM | WASM32 | X64 | UNKNOWN_PLATFORM
  static boolean platform_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "platform_")) return false;
    boolean r;
    r = consumeToken(b, ANDROID);
    if (!r) r = consumeToken(b, ANDROID_ARM32);
    if (!r) r = consumeToken(b, ANDROID_ARM64);
    if (!r) r = consumeToken(b, ARM32);
    if (!r) r = consumeToken(b, ARM64);
    if (!r) r = consumeToken(b, IOS);
    if (!r) r = consumeToken(b, IOS_ARM32);
    if (!r) r = consumeToken(b, IOS_ARM64);
    if (!r) r = consumeToken(b, IOS_X64);
    if (!r) r = consumeToken(b, LINUX);
    if (!r) r = consumeToken(b, LINUX_ARM32_HFP);
    if (!r) r = consumeToken(b, LINUX_MIPS32);
    if (!r) r = consumeToken(b, LINUX_MIPSEL32);
    if (!r) r = consumeToken(b, LINUX_X64);
    if (!r) r = consumeToken(b, MACOS_X64);
    if (!r) r = consumeToken(b, MINGW);
    if (!r) r = consumeToken(b, MINGW_X64);
    if (!r) r = consumeToken(b, MIPS32);
    if (!r) r = consumeToken(b, MIPSEL32);
    if (!r) r = consumeToken(b, OSX);
    if (!r) r = consumeToken(b, WASM);
    if (!r) r = consumeToken(b, WASM32);
    if (!r) r = consumeToken(b, X64);
    if (!r) r = consumeToken(b, UNKNOWN_PLATFORM);
    return r;
  }

  /* ********************************************************** */
  // definitionItem_* (DELIM code)?
  static boolean root(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "root")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = root_0(b, l + 1);
    r = r && root_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // definitionItem_*
  private static boolean root_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "root_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!definitionItem_(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "root_0", c)) break;
    }
    return true;
  }

  // (DELIM code)?
  private static boolean root_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "root_1")) return false;
    root_1_0(b, l + 1);
    return true;
  }

  // DELIM code
  private static boolean root_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "root_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DELIM);
    r = r && code(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // stringKey SEPARATOR stringValue
  static boolean stringDefinition_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringDefinition_")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = stringKey(b, l + 1);
    r = r && consumeToken(b, SEPARATOR);
    r = r && stringValue(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // extensibleStringKey_ platform_? | PACKAGE
  public static boolean stringKey(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringKey")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STRING_KEY, "<string key>");
    r = stringKey_0(b, l + 1);
    if (!r) r = consumeToken(b, PACKAGE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // extensibleStringKey_ platform_?
  private static boolean stringKey_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringKey_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = extensibleStringKey_(b, l + 1);
    r = r && stringKey_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // platform_?
  private static boolean stringKey_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringKey_0_1")) return false;
    platform_(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // VALUE
  public static boolean stringValue(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringValue")) return false;
    if (!nextTokenIs(b, VALUE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, VALUE);
    exit_section_(b, m, STRING_VALUE, r);
    return r;
  }

  /* ********************************************************** */
  // stringsKey platform_? SEPARATOR stringsValue
  static boolean stringsDefinition_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringsDefinition_")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = stringsKey(b, l + 1);
    r = r && stringsDefinition__1(b, l + 1);
    r = r && consumeToken(b, SEPARATOR);
    r = r && stringsValue(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // platform_?
  private static boolean stringsDefinition__1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringsDefinition__1")) return false;
    platform_(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // COMPILER_OPTS | DEPENDS  | ENTRY_POINT | EXCLUDED_FUNCTIONS
  //                             | EXCLUDED_MACROS  | EXPORT_FORWARD_DECLARATIONS | HEADER_FILTER
  //                             | HEADERS | LIBRARY_PATHS | LINKER_OPTS | MODULES | NON_STRICT_ENUMS
  //                             | NO_STRING_CONVERSION | PACKAGE | STATIC_LIBRARIES | STRICT_ENUMS
  public static boolean stringsKey(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringsKey")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STRINGS_KEY, "<strings key>");
    r = consumeToken(b, COMPILER_OPTS);
    if (!r) r = consumeToken(b, DEPENDS);
    if (!r) r = consumeToken(b, ENTRY_POINT);
    if (!r) r = consumeToken(b, EXCLUDED_FUNCTIONS);
    if (!r) r = consumeToken(b, EXCLUDED_MACROS);
    if (!r) r = consumeToken(b, EXPORT_FORWARD_DECLARATIONS);
    if (!r) r = consumeToken(b, HEADER_FILTER);
    if (!r) r = consumeToken(b, HEADERS);
    if (!r) r = consumeToken(b, LIBRARY_PATHS);
    if (!r) r = consumeToken(b, LINKER_OPTS);
    if (!r) r = consumeToken(b, MODULES);
    if (!r) r = consumeToken(b, NON_STRICT_ENUMS);
    if (!r) r = consumeToken(b, NO_STRING_CONVERSION);
    if (!r) r = consumeToken(b, PACKAGE);
    if (!r) r = consumeToken(b, STATIC_LIBRARIES);
    if (!r) r = consumeToken(b, STRICT_ENUMS);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // VALUE
  public static boolean stringsValue(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringsValue")) return false;
    if (!nextTokenIs(b, VALUE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, VALUE);
    exit_section_(b, m, STRINGS_VALUE, r);
    return r;
  }

}
