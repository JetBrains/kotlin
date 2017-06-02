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

if not "%JAVA_HOME%"=="" (
  if exist "%JAVA_HOME%\bin\java.exe" set "_JAVACMD=%JAVA_HOME%\bin\java.exe"
)

if "%_JAVACMD%"=="" set _JAVACMD=java

set OUTPUT_FILE_NAME=nativelib
set TARGET=host
set JAVA_ARGS=
set INTEROP_ARGS=

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
    if "!ARG!" == "-o" (
        set "OUTPUT_FILE_NAME=%2"
        shift
        goto next
    )
    if "!ARG!" == "-target" (
        set "TARGET=%2"
        shift
        goto next
    )

    set "INTEROP_ARGS=%INTEROP_ARGS% %ARG%"

    :next
    shift
    goto again
)

set "NATIVE_LIB=%_KONAN_HOME%\konan\nativelib"
set JAVA_OPTS=-ea ^
    "-Djava.library.path=%NATIVE_LIB%" ^
    "-Dkonan.home=%_KONAN_HOME%" ^
    -Dfile.encoding=UTF-8

set "STUB_GENERATOR_JAR=%_KONAN_HOME%\konan\lib\StubGenerator.jar"
set "KOTLIN_JAR=%_KONAN_HOME%\konan\lib\kotlin-compiler.jar"
set "INTEROP_INDEXER_JAR=%_KONAN_HOME%\konan\lib\Indexer.jar"
set "INTEROP_RUNTIME_JAR=%_KONAN_HOME%\konan\lib\Runtime.jar"
set "HELPERS_JAR=%_KONAN_HOME%\konan\lib\helpers.jar"
set "INTEROP_CLASSPATH=%STUB_GENERATOR_JAR%;%KOTLIN_JAR%;%INTEROP_INDEXER_JAR%;%INTEROP_RUNTIME_JAR%;%HELPERS_JAR%"
set "INTEROP_TOOL=org.jetbrains.kotlin.native.interop.gen.jvm.MainKt"

set "FLAVOR_ARG=-flavor native"

set "GENERATED_DIR=%OUTPUT_FILE_NAME%-build/kotlin"
set "GENERATED_ARG=-generated "%GENERATED_DIR%""
set "NATIVES_DIR=%OUTPUT_FILE_NAME%-build/natives"
set "NATIVES_ARG=-natives "%NATIVES_DIR%""
set "CSTUBSNAME=cstubs"
set "CSTUBSNAME_ARG=-cstubsname %CSTUBSNAME%"

set LIBCLANG_DISABLE_CRASH_RECOVERY=1

"%_JAVACMD%" %JAVA_OPTS% %JAVA_ARGS% ^
    -cp "%INTEROP_CLASSPATH%" ^
    %INTEROP_TOOL% ^
    %GENERATED_ARG% %NATIVES_ARG% %CSTUBSNAME_ARG% ^
    %FLAVOR_ARG% -target "%TARGET%" %INTEROP_ARGS%

if ERRORLEVEL 1 exit /b %ERRORLEVEL%

rem Stubs may be rather big, so we may need more heap space.
set XMX_ARG=-J-Xmx3G

call "%_KONAN_HOME%\bin\konanc.bat" %XMX_ARG% "%GENERATED_DIR%" -produce library ^
    -nativelibrary "%NATIVES_DIR%/%CSTUBSNAME%.bc" ^
    -o "%OUTPUT_FILE_NAME%" -target "%TARGET%"


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
  set "PATH=%_KONAN_HOME%\dependencies\msys2-mingw-w64-x86_64-gcc-6.3.0-clang-llvm-3.9.1-windows-x86-64\bin;%PATH%"
goto :eof

:end
endlocal

