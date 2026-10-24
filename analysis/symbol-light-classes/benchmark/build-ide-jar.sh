#!/bin/bash
# usage: build-ide-jar.sh <label> <kotlin-commit>
# Builds symbol-light-classes-for-ide from the sources of the given commit (SLC, light-classes-base, light classes for decompiled)
# and stores the jar as $S/ijjars/<label>.jar. The working tree sources are restored to HEAD afterwards.
T=$(cd "$(dirname "$0")" && pwd)
S=${SLC_BENCH_WORK:?set SLC_BENCH_WORK to a work directory, see README.md}
IJ=${INTELLIJ_HOME:-$HOME/code/intellij}
mkdir -p $S/bench $S/ij $S/jfr $S/ijjars
label=$1; commit=$2
cd "$T/../../.." || exit 1
paths=(analysis/symbol-light-classes/src analysis/light-classes-base/src analysis/decompiled/light-classes-for-decompiled/src)
git checkout -q $commit -- "${paths[@]}" || exit 1
rm -rf $S/ijjars/repo-$label
./gradlew :prepare:ide-plugin-dependencies:symbol-light-classes-for-ide:publish -Ppublish.ide.plugin.dependencies=true \
    -PdeployVersion=2.5.255-dev-255 -Pbuild.number=2.5.255-dev-255 -Pkotlin.build.deploy-path=$S/ijjars/repo-$label -q > $S/ijjars/build-$label.log 2>&1
rc=$?
git checkout -q HEAD -- "${paths[@]}"
jar=$(find $S/ijjars/repo-$label -name 'symbol-light-classes-for-ide-2.5.255-dev-255.jar' | head -1)
[ $rc -eq 0 ] && [ -n "$jar" ] && cp "$jar" $S/ijjars/$label.jar && echo "built $label from $commit: $(shasum $S/ijjars/$label.jar | cut -c1-12)" || { echo "build of $label failed rc=$rc"; tail -20 $S/ijjars/build-$label.log; }
