#!/bin/bash
# usage: ij-bench.sh <label> <jar-label> [rounds] [jfr]
# Installs the given SLC jar into IntelliJ and runs SymbolLightClassesJavaHighlightingBenchmarkTest.
T=$(cd "$(dirname "$0")" && pwd)
S=${SLC_BENCH_WORK:?set SLC_BENCH_WORK to a work directory, see README.md}
IJ=${INTELLIJ_HOME:-$HOME/code/intellij}
mkdir -p $S/bench $S/ij $S/jfr $S/ijjars
label=$1; jar=$2; rounds=${3:-5}; mode=${4:-}
$T/use-ide-jar.sh $jar > /dev/null || exit 1
opts="-Xmx6g -javaagent:$S/cpu-agent.jar=$S/ij/cpu.txt;$label"
if [ "$mode" = jfr ]; then
  mkdir -p $S/jfr/$label; rm -f $S/jfr/$label/*.jfr $S/jfr/$label.cpu.collapsed $S/jfr/$label.alloc.collapsed
  opts="$opts -XX:FlightRecorderOptions:stackdepth=2048 -XX:StartFlightRecording:settings=$T/profile-1ms.jfc,filename=$S/jfr/$label/test.jfr,dumponexit=true,maxsize=20g"
fi
cd "$IJ" || exit 1
./tests.cmd --module kotlin.fir.tests --test 'org.jetbrains.kotlin.idea.fir.javaFacade.SymbolLightClassesJavaHighlightingBenchmarkTest' \
  "-Dintellij.build.test.jvm.memory.options=$opts" "-Dpass.slc.bench.dir=$S/ijbench" "-Dpass.slc.bench.out=$S/ij/ijbench.tsv" \
  "-Dpass.slc.bench.label=$label" "-Dpass.slc.bench.rounds=$rounds" > $S/ij/$label.log 2>&1
rc=$?
echo "=== $label jar=$jar rc=$rc"
grep -E 'SLC benchmark|Results:|FAILED|error:' $S/ij/$label.log | sed 's/^.*SLC benchmark/SLC benchmark/' | head -40
grep "^$label " $S/ij/cpu.txt | tail -1
if [ "$mode" = jfr ]; then for f in $S/jfr/$label/*.jfr; do java -Xmx12g $T/JfrAgg.java "$f" "$S/jfr/$label" '.'; done; fi
