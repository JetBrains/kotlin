#
# Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

# Usage: compiler/testData/diagnostics/nativeTests/specialBackendChecks/runtest.sh <t??.kt> <-language-version 1.9>

FILTERED_FILE=/tmp/$(basename $1)
# Remove diagnostic directives from test source
cat $1 | sed -e 's/<![a-zA-Z_!]*!>//g' | sed -e 's/<!>//g' > $FILTERED_FILE
shift
konanc -opt-in=kotlin.native.internal.InternalForKotlinNative "$FILTERED_FILE" "$@"
