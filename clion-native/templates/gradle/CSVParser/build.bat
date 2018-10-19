@echo off

setlocal

set DIR=.

if defined KONAN_HOME (
    set "PATH=%KONAN_HOME%\bin;%PATH%"
) else (
    set "PATH=..\..\dist\bin;..\..\bin;%PATH%"
)

if "%TARGET%" == "" set TARGET=mingw

call konanc -target "%TARGET%" "%DIR%\src\main\kotlin\CsvParser.kt" -o CsvParser
exit /b %ERRORLEVEL%
