#!/bin/sh

if [ "$#" -lt 4 ]; then
  echo "Usage: $0 <bootstrap-compiler-repo-path> <bootstrap-compiler-dist-path> <bootstrap-compiler-version> <model-dump-dir>" >&2
  exit 1
fi

bootstrap_repo="$1"
bootstrap_dist="$2"
bootstrap_ver="$3"
model_dump="$4"

./gradlew clean tasks -Pbootstrap.local=true -Pbootstrap.local.path="$bootstrap_repo" -Pbootstrap.kotlin.version="$bootstrap_ver"

mkdir -p "$model_dump"

export KOTLIN_DUMP_MODEL="$model_dump"
./gradlew --no-build-cache compileKotlin compileTestKotlin -Pbootstrap.local=true -Pbootstrap.local.path="$bootstrap_repo" -Pbootstrap.kotlin.version="$bootstrap_ver"

rm -f "$model_dump"/*-plugins-blocks.*
rm -f "$model_dump"/model-kotlin-stdlib.xml
rm -f "$model_dump"/model-kotlin-stdlib_compileOnlyDeclarations.xml
rm -f "$model_dump"/model-kotlin-test.xml
rm -f "$model_dump"/model-kotlin-test_JUnit.xml
rm -f "$model_dump"/model-kotlin-test_JUnit5.xml
