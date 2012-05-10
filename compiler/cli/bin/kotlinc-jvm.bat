rem based on scalac.bat from the Scala distribution
rem ##########################################################################
rem # Copyright 2002-2011, LAMP/EPFL
rem # Copyright 2011, JetBrains
rem #
rem # This is free software; see the distribution for copying conditions.
rem # There is NO warranty; not even for MERCHANTABILITY or FITNESS FOR A
rem # PARTICULAR PURPOSE.
rem ##########################################################################
@echo off

@setlocal
call :set_home

if not "%JAVA_HOME%"=="" (
  if exist "%JAVA_HOME%\bin\java.exe" set "_JAVACMD=%JAVA_HOME%\bin\java.exe"
)

if "%_JAVACMD%"=="" set _JAVACMD=java

rem We use the value of the JAVA_OPTS environment variable if defined
set _JAVA_OPTS=-Xmx256M -Xms32M

"%_JAVACMD%" %_JAVA_OPTS% -cp "%_KOTLIN_HOME%\lib\kotlin-compiler.jar" org.jetbrains.jet.cli.jvm.K2JVMCompiler  %*
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
