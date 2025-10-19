#!/bin/bash
/Users/Simon.Ogorodnik/Workspace/KotlinRepo/work2/compiler/fir/modularized-tests/build/libs/owo-2.3.255-SNAPSHOT \
-DcacheRedirectorEnabled=true \
-Dfir.bench.dump=false \
-Dfir.bench.jps.dir=/Users/Simon.Ogorodnik/TestWorkspace/okhttp-bench/modelDump \
-Dfir.bench.passes=1 \
-Dfir.bench.prefix=/ \
-Didea.home.path=/Users/Simon.Ogorodnik/Workspace/KotlinRepo/work2/build/ideaHomeForTests \
-Didea.ignore.disabled.plugins=true \
-Didea.is.unit.test=true \
-Didea.use.native.fs.for.win=false \
-Djava.awt.headless=true \
-Djna.nosys=true \
-Djps.kotlin.home=/Users/Simon.Ogorodnik/Workspace/KotlinRepo/work2/dist/kotlinc \
-Xmx8192m \
-Xms8192m