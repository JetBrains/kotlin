@echo off
rem based on scalac.bat from the Scala distribution
rem ##########################################################################
rem # Copyright 2002-2011, LAMP/EPFL
rem # Copyright 2011-2013, JetBrains
rem #
rem # This is free software; see the distribution for copying conditions.
rem # There is NO warranty; not even for MERCHANTABILITY or FITNESS FOR A
rem # PARTICULAR PURPOSE.
rem ##########################################################################

rem We adopt the following conventions:
rem - System/user environment variables start with a letter
rem - Local batch variables start with an underscore ('_')

@setlocal
call :set_home

if not "%JAVA_HOME%"=="" (
  if exist "%JAVA_HOME%\bin\java.exe" set "_JAVACMD=%JAVA_HOME%\bin\java.exe"
)

if "%_JAVACMD%"=="" set _JAVACMD=java

set _PROFILE_PRELOADER=notime
rem re: E%1 &c., see http://www.riedquat.de/blog/2011-11-27-01#w9aab1c45
if E%1==E-pp (
  shift
  set _PROFILE_PRELOADER=time
)

rem We use the value of the JAVA_OPTS environment variable if defined
set _JAVA_OPTS=-Xmx256M -Xms32M -noverify

rem re: %1 %2 &c., see http://www.riedquat.de/blog/2011-11-27-01#w9aab1c60
if "%_PROFILE_PRELOADER%"=="time" (
  "%_JAVACMD%" %_JAVA_OPTS% -cp "%_KOTLIN_HOME%\lib\kotlin-preloader.jar" ^
    org.jetbrains.jet.preloading.Preloader "%_KOTLIN_HOME%\lib\kotlin-compiler.jar;%_KOTLIN_HOME%\lib\kotlin-runtime.jar" ^
    org.jetbrains.jet.cli.jvm.K2JVMCompiler 4096 %_PROFILE_PRELOADER% %1 %2 %3 %4 %5 %6 %7 %8 %9
) else (
  "%_JAVACMD%" %_JAVA_OPTS% -cp "%_KOTLIN_HOME%\lib\kotlin-preloader.jar" ^
    org.jetbrains.jet.preloading.Preloader "%_KOTLIN_HOME%\lib\kotlin-compiler.jar;%_KOTLIN_HOME%\lib\kotlin-runtime.jar" ^
    org.jetbrains.jet.cli.jvm.K2JVMCompiler 4096 %_PROFILE_PRELOADER% %*
)
exit /b %ERRORLEVEL%
goto end

rem ##########################################################################
rem # subroutines

:set_home
  set _BIN_DIR=
  for %%i in (%~sf0) do set _BIN_DIR=%_BIN_DIR%%%~dpsi
  set _KOTLIN_HOME=%_BIN_DIR%..
goto :eof

:end
@endlocal
