@echo off
rem based on scalac.bat from the Scala distribution
rem ##########################################################################
rem # Copyright 2002-2011, LAMP/EPFL
rem # Copyright 2011-2017, JetBrains
rem #
rem # This is free software; see the distribution for copying conditions.
rem # There is NO warranty; not even for MERCHANTABILITY or FITNESS FOR A
rem # PARTICULAR PURPOSE.
rem ##########################################################################

setlocal enabledelayedexpansion
call :set_home
call :set_path

set "TOOL_NAME=%1"
shift

if "%_TOOL_CLASS%"=="" set _TOOL_CLASS=org.jetbrains.kotlin.cli.utilities.MainKt

if not "%JAVA_HOME%"=="" (
  if exist "%JAVA_HOME%\bin\java.exe" set "_JAVACMD=%JAVA_HOME%\bin\java.exe"
)

if "%_JAVACMD%"=="" set _JAVACMD=java

set JAVA_ARGS=
set KONAN_ARGS=

:again
set "ARG=%1"
if not "!ARG!" == "" (
    if "!ARG:~0,2!" == "-D" (
        set "JAVA_ARGS=%JAVA_ARGS% %ARG%"
        goto next
    )
    if "!ARG:~0,2!" == "-J" (
        set "JAVA_ARGS=%JAVA_ARGS% !ARG:~2!"
        goto next
    )
    if "!ARG!" == "--time" (
        set "KONAN_ARGS=%KONAN_ARGS% --time"
        set "JAVA_ARGS=%JAVA_ARGS% -Dkonan.profile=true"
        goto next
    )

    set "KONAN_ARGS=%KONAN_ARGS% %ARG%"

    :next
    shift
    goto again
)

set "NATIVE_LIB=%_KONAN_HOME%\konan\nativelib"
set "KONAN_LIB=%_KONAN_HOME%\konan\lib"

set "SHARED_JAR=%KONAN_LIB%\kotlin-native-shared.jar"
set "EXTRACTED_METADATA_JAR=%KONAN_LIB%\konan.metadata.jar"
set "EXTRACTED_SERIALIZER_JAR=%KONAN_LIB%\konan.serializer.jar"
set "INTEROP_INDEXER_JAR=%KONAN_LIB%\Indexer.jar"
set "INTEROP_RUNTIME_JAR=%KONAN_LIB%\Runtime.jar"
set "KLIB_JAR=%KONAN_LIB%\klib.jar"
set "KONAN_JAR=%KONAN_LIB%\backend.native.jar"
set "KOTLIN_JAR=%KONAN_LIB%\kotlin-compiler.jar"
set "KOTLIN_STDLIB_JAR=%KONAN_LIB%\kotlin-stdlib.jar"
set "KOTLIN_REFLECT_JAR=%KONAN_LIB%\kotlin-stdlib.jar"
set "KOTLIN_SCRIPT_RUNTIME_JAR=%KONAN_LIB%\kotlin-script-runtime.jar"
set "STUB_GENERATOR_JAR=%KONAN_LIB%\StubGenerator.jar"
set "UTILITIES_JAR=%KONAN_LIB%\utilities.jar"
set TROVE_JAR="%KONAN_LIB%\lib\trove4j.jar"
set "VERSION_JAR=%KONAN_LIB%\version.jar"

set "KONAN_CLASSPATH=%KOTLIN_JAR%;%KOTLIN_STDLIB_JAR%;%KOTLIN_REFLECT_JAR%;%KOTLIN_SCRIPT_RUNTIME_JAR%;%INTEROP_RUNTIME_JAR%;%KONAN_JAR%;%STUB_GENERATOR_JAR%;%INTEROP_INDEXER_JAR%;%SHARED_JAR%;%EXTRACTED_METADATA_JAR%;%EXTRACTED_SERIALIZER_JAR%;%KLIB_JAR%;%UTILITIES_JAR%;%TROVE_JAR%;%VERSION_JAR%"

set JAVA_OPTS=-ea ^
    -Xmx3G ^
    -XX:TieredStopAtLevel=1 ^
    "-Djava.library.path=%NATIVE_LIB%" ^
    "-Dkonan.home=%_KONAN_HOME%" ^
    -Dfile.encoding=UTF-8 ^
    %JAVA_OPTS%

set LIBCLANG_DISABLE_CRASH_RECOVERY=1

"%_JAVACMD%" %JAVA_OPTS% %JAVA_ARGS% -cp "%KONAN_CLASSPATH%" %_TOOL_CLASS% %TOOL_NAME% %KONAN_ARGS%

exit /b %ERRORLEVEL%
goto end

rem ##########################################################################
rem # subroutines

:set_home
  set _BIN_DIR=
  for %%i in (%~sf0) do set _BIN_DIR=%_BIN_DIR%%%~dpsi
  set _KONAN_HOME=%_BIN_DIR%..
goto :eof

:set_path
  rem libclang.dll is dynamically linked and thus requires correct PATH to be loaded.
  rem TODO: remove this hack.
  if "%KONAN_DATA_DIR%"=="" (set KONAN_DATA_DIR=%USERPROFILE%\.konan)
  set "PATH=%KONAN_DATA_DIR%\dependencies\msys2-mingw-w64-x86_64-gcc-7.3.0-clang-llvm-lld-6.0.1-2\bin;%PATH%"
goto :eof

:end
endlocal
