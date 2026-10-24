#!/bin/bash
# usage: use-ide-jar.sh <label>: installs $S/ijjars/<label>.jar into the IntelliJ kotlin-snapshot repository
T=$(cd "$(dirname "$0")" && pwd)
S=${SLC_BENCH_WORK:?set SLC_BENCH_WORK to a work directory, see README.md}
IJ=${INTELLIJ_HOME:-$HOME/code/intellij}
mkdir -p $S/bench $S/ij $S/jfr $S/ijjars
dst=$IJ/community/lib/kotlin-snapshot/org/jetbrains/kotlin/symbol-light-classes-for-ide/2.5.255-dev-255/symbol-light-classes-for-ide-2.5.255-dev-255.jar
[ -f $S/ijjars/orig.jar ] || cp $dst $S/ijjars/orig.jar
cp $S/ijjars/$1.jar $dst && echo "installed $1: $(shasum $dst | cut -c1-12)"
