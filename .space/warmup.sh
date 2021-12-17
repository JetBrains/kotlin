#!/bin/sh
set -e -x -u
# This file is used in .space.kts (located at the root of the repository).

cat <<EOF > local.properties
kotlin.native.enabled=true
EOF

./gradlew assemble dist
