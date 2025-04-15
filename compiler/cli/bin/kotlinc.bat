@echo off

rem Based on scalac.bat from the Scala distribution
rem Copyright 2002-2011, LAMP/EPFL
rem Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
rem Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

rem We adopt the following conventions:
rem - System/user environment variables start with a letter
rem - Local batch variables start with an underscore ('_')

setlocal
call :set_home

if "%_KOTLIN_COMPILER%"=="" set _KOTLIN_COMPILER=org.jetbrains.kotlin.cli.jvm.K2JVMCompiler 

if not "%JAVA_HOME%"=="" (
  rem Prepend JAVA_HOME to local PATH to be able to simply execute "java" later in the script.
  set "PATH=%JAVA_HOME%\bin;%PATH%"
)

rem We use the value of the JAVA_OPTS environment variable if defined
if "%JAVA_OPTS%"=="" set JAVA_OPTS=-Xmx512M -Xms128M

rem Iterate through arguments and split them into java and kotlin ones
:loop
set _arg=%~1
if "%_arg%" == "" goto loopend

if "%_arg:~0,2%"=="-J" (
  if "%_arg:~2%"=="" (
    echo error: empty -J argument
    goto error
  )
  set JAVA_OPTS=%JAVA_OPTS% "%_arg:~2%"
) else (
  if "%_arg:~0,2%"=="-D" (
    set JAVA_OPTS=%JAVA_OPTS% "%_arg%"
  ) else (
    set KOTLIN_OPTS=%KOTLIN_OPTS% "%_arg%"
  )
)
shift
goto loop
:loopend

setlocal EnableDelayedExpansion

call :set_java_version

if !_java_major_version! geq 24 (
  rem Allow JNI access for all compiler code. In particular, this is needed for jansi (see `PlainTextMessageRenderer`).
  set JAVA_OPTS=!JAVA_OPTS! "--enable-native-access=ALL-UNNAMED"

  rem Suppress unsafe deprecation warnings, see KT-76799 and IDEA-370928.
  set JAVA_OPTS=!JAVA_OPTS! "--sun-misc-unsafe-memory-access=allow"
)

if "!_KOTLIN_RUNNER!"=="1" (
  java !JAVA_OPTS! "-Dkotlin.home=%_KOTLIN_HOME%" -cp "%_KOTLIN_HOME%\lib\kotlin-runner.jar" ^
    org.jetbrains.kotlin.runner.Main %KOTLIN_OPTS%
) else (
  set _ADDITIONAL_CLASSPATH=

  if !_java_major_version! lss 13 (
    set JAVA_OPTS=!JAVA_OPTS! "-noverify"
  )

  if not "%_KOTLIN_TOOL%"=="" (
    set _ADDITIONAL_CLASSPATH=;%_KOTLIN_HOME%\lib\%_KOTLIN_TOOL%
  )

  java !JAVA_OPTS! -cp "%_KOTLIN_HOME%\lib\kotlin-preloader.jar" ^
    org.jetbrains.kotlin.preloading.Preloader -cp "%_KOTLIN_HOME%\lib\kotlin-compiler.jar!_ADDITIONAL_CLASSPATH!" ^
    %_KOTLIN_COMPILER% %KOTLIN_OPTS%
)

goto end

rem ##########################################################################
rem # subroutines

:set_home
  set _BIN_DIR=
  for %%i in (%~sf0) do set _BIN_DIR=%_BIN_DIR%%%~dpsi
  set _KOTLIN_HOME=%_BIN_DIR%..
goto :eof

rem Parses "java -version" output and stores the major version to _java_major_version.
rem Note that this only loads the first component of the version, so "1.8.0_265" -> "1".
rem But it's fine because major version is 9 for JDK 9, and so on.
rem Needs to be executed in the EnableDelayedExpansion mode.
:set_java_version
  set _version=
  rem Parse output and take the third token from the string containing " version ".
  rem It should be something like "1.8.0_275" or "15.0.1".
  for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i " version "') do (
    rem Split the string by "-" or "." and take the first token.
    for /f "delims=-. tokens=1" %%j in ("%%i") do (
      rem At this point, _version should be something like "1 or "15. Note the leading quote.
      set _version=%%j
    )
  )
  if "!_version!"=="" (
    rem If failed to parse the output, set the version to 1.
    set _java_major_version=1
  ) else (
    rem Strip the leading quote.
    set _java_major_version=!_version:~1!
  )
goto :eof

:error
set ERRORLEVEL=1

:end
exit /b %ERRORLEVEL%
