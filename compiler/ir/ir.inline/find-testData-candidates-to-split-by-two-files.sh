#
# Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

# Prints codegen/box testData, which are good candidates to be splitted to two source files, in order to test cross-module inliner.
# - test has top-level `inline fun`
# - test is not yet split with `// FILE:` directives
# - test is not targeted for only JVM* backend

# After getting list of these files, `// FILE:` test directives can be easily inserted by Junie(default model) using the following prompt.
# Specify not more than 40 files at once, for efficient processing.
# Review the changes carefully.
# For each changed file, run all tests using DevKit

# --- Junie prompt start ---
  # Into each of the following tests
  # compiler/testData/codegen/box/<TEST1>.kt
  # compiler/testData/codegen/box/<TEST2>.kt
  #
  # insert two test directives
  # - `// FILE: lib.kt`
  # - `// FILE: main.kt`
  # to mark a need for a further processing to split to two compilable source files.
  # Goal:
  # After directive `// FILE: lib.kt` there should be definitions of all inline functions, and declarations they use
  # After directive `// FILE: main.kt` there should be all other declarations including `box()` function.
  # Don't insert new directives before first lines starting with `//`
  # In case more changes are needed, than just couple lines insertion, place new `lib` section before `main` section.
  # See examples of such test directives in tests:
  # - compiler/testData/codegen/box/casts/kt54707.kt
  # - compiler/testData/codegen/box/casts/kt54802.kt
  # - compiler/testData/codegen/box/casts/kt55005.kt
# --- Junie prompt end ---

find compiler/testData/codegen/box -name "*.kt" -type f | while read -r file; do
    if grep -q "^inline fun" "$file" &&
      ! grep -q "^// FILE:" "$file" &&
      ! grep -q "^// MODULE:" "$file" &&
      ! grep -q "^// TARGET_BACKEND: JVM$" "$file" &&
      ! grep -q "^// TARGET_BACKEND: JVM_IR$" "$file"
    then
        echo "$file"
    fi
done