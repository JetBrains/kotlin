#!/bin/bash
# usage: ij-suites-ab.sh <series> <rounds> <jarA> <jarB>
# Alternates the SLC jars A and B and runs three IntelliJ suites with the CPU agent; results go to $S/ij/cpu.txt.
T=$(cd "$(dirname "$0")" && pwd)
S=${SLC_BENCH_WORK:?set SLC_BENCH_WORK to a work directory, see README.md}
IJ=${INTELLIJ_HOME:-$HOME/code/intellij}
mkdir -p $S/bench $S/ij $S/jfr $S/ijjars
series=$1; rounds=$2; a=$3; b=$4
suites=(
  "fu|kotlin.fir.tests|org.jetbrains.kotlin.idea.fir.findUsages.FindUsagesFirTestGenerated*"
  "uast|intellij.kotlin.uast.tests|org.jetbrains.fir.uast.test.Fir*TestGenerated*"
  "jk|intellij.kotlin.codeInsight.tests|org.jetbrains.kotlin.idea.k2.K2JavaAgainstKotlinSourceCheckerTestGenerated*"
)
for i in $(seq 1 $rounds); do
  for st in A B; do
    jar=$a; [ $st = B ] && jar=$b
    $T/use-ide-jar.sh $jar > /dev/null
    for s in "${suites[@]}"; do
      IFS='|' read -r name module pattern <<< "$s"
      $T/ij-jfr.sh $series-$name-$st$i "$module" "$pattern" nojfr | grep -E '===|Results|cpu='
    done
  done
done
$T/use-ide-jar.sh master
