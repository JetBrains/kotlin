#!/bin/bash
# usage: ij-jfr.sh <label> <module> <test-pattern> [jfr|nojfr]
# Runs IntelliJ tests with the CPU agent (and deep-stack 1 ms JFR unless "nojfr"); aggregates all threads into
# $S/jfr/<label>.cpu.collapsed and $S/jfr/<label>.alloc.collapsed.
T=$(cd "$(dirname "$0")" && pwd)
S=${SLC_BENCH_WORK:?set SLC_BENCH_WORK to a work directory, see README.md}
IJ=${INTELLIJ_HOME:-$HOME/code/intellij}
mkdir -p $S/bench $S/ij $S/jfr $S/ijjars
label=$1; module=$2; pattern=$3; mode=${4:-jfr}
mkdir -p $S/ij $S/jfr/$label
opts="-Xmx6g -javaagent:$S/cpu-agent.jar=$S/ij/cpu.txt;$label"
if [ "$mode" = jfr ]; then
  rm -f $S/jfr/$label/*.jfr $S/jfr/$label.cpu.collapsed $S/jfr/$label.alloc.collapsed
  opts="$opts -XX:FlightRecorderOptions:stackdepth=2048 -XX:StartFlightRecording:settings=$T/profile-1ms.jfc,filename=$S/jfr/$label/test.jfr,dumponexit=true,maxsize=20g"
fi
cd "$IJ" || exit 1
start=$(date +%s)
./tests.cmd --module "$module" --test "$pattern" "-Dintellij.build.test.jvm.memory.options=$opts" > $S/ij/$label.log 2>&1
rc=$?
echo "=== $label rc=$rc wall=$(( $(date +%s) - start ))s"
grep -E '^Results:' $S/ij/$label.log
grep "^$label " $S/ij/cpu.txt | tail -1
if [ "$mode" = jfr ]; then
  for f in $S/jfr/$label/*.jfr; do java -Xmx12g $T/JfrAgg.java "$f" "$S/jfr/$label" '.'; done
fi
