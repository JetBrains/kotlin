#
# Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#
SOURCE=${BASH_SOURCE[0]}
SCRIPT_DIR=$(dirname $SOURCE)
for TEST in $SCRIPT_DIR/**/*.kt; do
  echo "$TEST"
  compiler/testData/diagnostics/nativeTests/specialBackendChecks/runtest.sh $TEST "$@"
done
