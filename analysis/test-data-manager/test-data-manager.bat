@rem
@rem Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
@rem Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
@rem

@rem Runs the test data manager wrapper on Windows; see `test-data-manager` next to this script for how it is built.

@echo off
setlocal

for %%i in ("%~dp0..\..") do set "ROOT=%%~fi"
set "SOURCES=analysis/test-data-manager"
set "CATALOG=gradle/libs.versions.toml"
set "BUILDS=%ROOT%\build\test-data-manager\cli"

set "INSTALLED=%ROOT%\analysis\test-data-manager\test-data-manager-cli\build\cli"

set "DIRTY="
for /f "delims=" %%s in ('git -C "%ROOT%" status --porcelain -- %SOURCES% %CATALOG%') do set "DIRTY=1"
if defined DIRTY (
    call :install || exit /b 2
    set "BUILD=%INSTALLED%"
    goto run
)

set "KEY="
for /f "delims=" %%k in ('git -C "%ROOT%" rev-parse HEAD:%SOURCES% HEAD:%CATALOG% ^| git hash-object --stdin') do set "KEY=%%k"
if not defined KEY (
    echo ERROR the git trees of %SOURCES% and %CATALOG% are unknown 1>&2
    exit /b 2
)

set "BUILD=%BUILDS%\%KEY%"
if exist "%BUILD%\complete" goto prune

call :install || exit /b 2
set "TEMPORARY=%BUILD%.%RANDOM%.tmp"
if exist "%TEMPORARY%" rmdir /s /q "%TEMPORARY%"
xcopy /e /i /q /y "%INSTALLED%" "%TEMPORARY%" >nul
if errorlevel 1 goto failedInstall
type nul > "%TEMPORARY%\complete"
if not exist "%TEMPORARY%\complete" goto failedInstall
rem Another run may have installed the same build meanwhile
if exist "%BUILD%" if not exist "%BUILD%\complete" rmdir /s /q "%BUILD%"
if exist "%BUILD%\complete" (rmdir /s /q "%TEMPORARY%") else (move "%TEMPORARY%" "%BUILD%" >nul)
if not exist "%BUILD%\complete" goto failedInstall

:prune
rem The 5 most recent builds are kept
for /f "skip=5 delims=" %%d in ('dir /b /ad /o-d "%BUILDS%" ^| findstr /v /e /c:".tmp"') do rmdir /s /q "%BUILDS%\%%d"

:run
set "JAVA_EXE=java.exe"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

rem UTF-8, as the wrapper reads the Gradle output it gets in the default charset
"%JAVA_EXE%" -XX:+UseSerialGC -Xshare:auto --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 ^
    "-Dorg.jetbrains.kotlin.testDataManager.cli.repositoryRoot=%ROOT%" ^
    -cp "%BUILD%\*" org.jetbrains.kotlin.analysis.test.data.manager.cli.TestDataManagerCliKt %*
exit /b %ERRORLEVEL%

:install
echo Building the test data manager wrapper 1>&2
pushd "%ROOT%"
call gradlew.bat -q :analysis:test-data-manager:test-data-manager-cli:installCli 1>&2
set "GRADLE_EXIT=%ERRORLEVEL%"
popd
if not "%GRADLE_EXIT%"=="0" (
    echo ERROR the test data manager wrapper failed to build 1>&2
    exit /b 1
)
exit /b 0

:failedInstall
echo ERROR the test data manager wrapper failed to install into %BUILD% 1>&2
if exist "%TEMPORARY%" rmdir /s /q "%TEMPORARY%"
exit /b 2
