@echo off

rem Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
rem Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

setlocal
set _KOTLIN_RUNNER=1

echo warning: the 'kotlin' executable is deprecated; use 'kotlinr' instead to avoid ambiguity with the Kotlin toolchain's 'kotlin' command. 1>&2

call "%~dp0kotlinc.bat" %*
