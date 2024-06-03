#!/bin/sh

DIRECTORY=“compoundSyntaxExamplesIf”

total_time=0

count=0

for file in $DIRECTORY/*.kt
do
    echo "Compiling $file"

    filename=$(basename -- "$file")
    filename="${filename%.*}"

    hyperfine -w 5 -r 10 --export-json "$filename.json" "./dist/kotlinc/bin/kotlinc $file"

    time=$(jq '.results[0].mean' "$filename.json")

    total_time=$(echo "scale=5; $total_time + $time" | bc)

    count=$((count + 1))

done


average=$(echo "scale=5; $total_time / $count" | bc)

echo "Average compile time is $average seconds"