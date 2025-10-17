#
# Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

# Prints codegen/box testData, which are good candidates to be splitted to two source files, in order to test cross-module inliner.
# - test has top-level `inline fun`
# - test is not yet split with `// FILE:` directives
# - test is not targeted for only JVM* backend

find compiler/testData/codegen/box -name "*.kt" -type f | while read -r file; do
    if grep -q "^inline fun" "$file" &&
      ! grep -q "^// FILE:" "$file" &&
      ! grep -q "^// TARGET_BACKEND: JVM$" "$file" &&
      ! grep -q "^// TARGET_BACKEND: JVM_IR$" "$file"
    then
        echo "$file"
    fi
done