package org.jetbrains.kotlin.ide.konan;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import org.jetbrains.kotlin.ide.konan.psi.NativeDefinitionsTypes;

%%

%class NativeDefinitionsLexer
%implements FlexLexer, NativeDefinitionsTypes
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

CRLF=\R
WHITE_SPACE=[\ \v\t\f]

SEPARATOR=[:=]
PLATFORM_CHAR=[:jletterdigit:]
KEY_CHAR=[^:=\ \r\n\t\f\\.] | "\\ "
FIRST_VALUE_CHAR=[^ \n\f\\] | "\\"{CRLF} | "\\".
VALUE_CHAR= [^\n\f\\] | "\\"{CRLF} | "\\".
COMMENT=("#"|"!")[^\r\n]*
DELIM=---{CRLF}

// known properties
COMPILER_OPTS="compilerOpts"
DEPENDS="depends"
DISABLE_DESIGNATED_INITIALIZER_CHECKS="disableDesignatedInitializerChecks"
ENTRY_POINT="entryPoint"
EXCLUDE_DEPENDENT_MODULES="excludeDependentModules"
EXCLUDED_FUNCTIONS="excludedFunctions"
EXCLUDED_MACROS="excludedMacros"
EXCLUDE_SYSTEM_LIBS="excludeSystemLibs"
EXPORT_FORWARD_DECLARATIONS="exportForwardDeclarations"
HEADER_FILTER="headerFilter"
HEADERS="headers"
LANGUAGE="language"
LIBRARY_PATHS="libraryPaths"
LINKER="linker"
LINKER_OPTS="linkerOpts"
MODULES="modules"
NON_STRICT_ENUMS="nonStrictEnums"
NO_STRING_CONVERSION="noStringConversion"
PACKAGE="package"
STATIC_LIBRARIES="staticLibraries"
STRICT_ENUMS="strictEnums"

// known platforms
ANDROID="android"
ANDROID_ARM32="android_arm32"
ANDROID_ARM64="android_arm64"
ARM32="arm32"
ARM64="arm64"
IOS="ios"
IOS_ARM32="ios_arm32"
IOS_ARM64="ios_arm64"
IOS_X64="ios_x64"
LINUX="linux"
LINUX_ARM32_HFP="linux_arm32_hfp"
LINUX_MIPS32="linux_mips32"
LINUX_MIPSEL32="linux_mipsel32"
LINUX_X64="linux_x64"
MACOS_X64="macos_x64"
MINGW="mingw"
MINGW_X64="mingw_x64"
MIPS32="mips32"
MIPSEL32="mipsel32"
OSX="osx"
WASM="wasm"
WASM32="wasm32"
X64="x64"


%state WAITING_PLATFORM
%state WAITING_VALUE
%state CODE_END

%%

{WHITE_SPACE}+ { return TokenType.WHITE_SPACE; }

<YYINITIAL> {
  {DELIM} { yybegin(CODE_END); return DELIM; }
  {COMMENT} { return COMMENT; }
  {COMPILER_OPTS} { yybegin(WAITING_PLATFORM); return COMPILER_OPTS; }
  {DEPENDS} { yybegin(WAITING_PLATFORM); return DEPENDS; }
  {DISABLE_DESIGNATED_INITIALIZER_CHECKS} { yybegin(WAITING_PLATFORM); return DISABLE_DESIGNATED_INITIALIZER_CHECKS; }
  {ENTRY_POINT} { yybegin(WAITING_PLATFORM); return ENTRY_POINT; }
  {EXCLUDE_DEPENDENT_MODULES} { yybegin(WAITING_PLATFORM); return EXCLUDE_DEPENDENT_MODULES; }
  {EXCLUDED_FUNCTIONS} { yybegin(WAITING_PLATFORM); return EXCLUDED_FUNCTIONS; }
  {EXCLUDED_MACROS} { yybegin(WAITING_PLATFORM); return EXCLUDED_MACROS; }
  {EXCLUDE_SYSTEM_LIBS} { yybegin(WAITING_PLATFORM); return EXCLUDE_SYSTEM_LIBS; }
  {EXPORT_FORWARD_DECLARATIONS} { yybegin(WAITING_PLATFORM); return EXPORT_FORWARD_DECLARATIONS; }
  {HEADER_FILTER} { yybegin(WAITING_PLATFORM); return HEADER_FILTER; }
  {HEADERS} { yybegin(WAITING_PLATFORM); return HEADERS; }
  {LANGUAGE} { yybegin(WAITING_PLATFORM); return LANGUAGE; }
  {LIBRARY_PATHS} { yybegin(WAITING_PLATFORM); return LIBRARY_PATHS; }
  {LINKER} { yybegin(WAITING_PLATFORM); return LINKER; }
  {LINKER_OPTS} { yybegin(WAITING_PLATFORM); return LINKER_OPTS; }
  {MODULES} { yybegin(WAITING_PLATFORM); return MODULES; }
  {NON_STRICT_ENUMS} { yybegin(WAITING_PLATFORM); return NON_STRICT_ENUMS; }
  {NO_STRING_CONVERSION} { yybegin(WAITING_PLATFORM); return NO_STRING_CONVERSION; }
  {PACKAGE} { yybegin(WAITING_PLATFORM); return PACKAGE; }
  {STATIC_LIBRARIES} { yybegin(WAITING_PLATFORM); return STATIC_LIBRARIES; }
  {STRICT_ENUMS} { yybegin(WAITING_PLATFORM); return STRICT_ENUMS; }
  {KEY_CHAR}+ { yybegin(WAITING_PLATFORM); return UNKNOWN_KEY; }
}

<WAITING_PLATFORM> {
  [.]{ANDROID} { return ANDROID; }
  [.]{ANDROID_ARM32} { return ANDROID_ARM32; }
  [.]{ANDROID_ARM64} { return ANDROID_ARM64; }
  [.]{ARM32} { return ARM32; }
  [.]{ARM64} { return ARM64; }
  [.]{IOS} { return IOS; }
  [.]{IOS_ARM32} { return IOS_ARM32; }
  [.]{IOS_ARM64} { return IOS_ARM64; }
  [.]{IOS_X64} { return IOS_X64; }
  [.]{LINUX} { return LINUX; }
  [.]{LINUX_ARM32_HFP} { return LINUX_ARM32_HFP; }
  [.]{LINUX_MIPS32} { return LINUX_MIPS32; }
  [.]{LINUX_MIPSEL32} { return LINUX_MIPSEL32; }
  [.]{LINUX_X64} { return LINUX_X64; }
  [.]{MACOS_X64} { return MACOS_X64; }
  [.]{MINGW} { return MINGW; }
  [.]{MINGW_X64} { return MINGW_X64; }
  [.]{MIPS32} { return MIPS32; }
  [.]{MIPSEL32} { return MIPSEL32; }
  [.]{OSX} { return OSX; }
  [.]{WASM} { return WASM; }
  [.]{WASM32} { return WASM32; }
  [.]{X64} { return X64; }
  [.]{PLATFORM_CHAR}+ { return UNKNOWN_PLATFORM; }
  {SEPARATOR} { yybegin(WAITING_VALUE); return SEPARATOR; }
}

<WAITING_VALUE> {FIRST_VALUE_CHAR}{VALUE_CHAR}* { yybegin(YYINITIAL); return VALUE; }

<CODE_END> [^]* { return CODE_CHARS; }

{CRLF}({CRLF}|{WHITE_SPACE})* { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }

[^\r\n\R] { yybegin(YYINITIAL); return TokenType.BAD_CHARACTER; }