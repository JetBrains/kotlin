#!/bin/bash
/Users/Simon.Ogorodnik/Workspace/KotlinRepo/main/compiler/fir/modularized-tests/build/libs/owo-2.1.255-SNAPSHOT \
-DcacheRedirectorEnabled=true \
-Dfir.bench.dump=false \
-Dfir.bench.jps.dir=/Users/Simon.Ogorodnik/TestWorkspace/mt-kotlin2/test-project-model-dump \
-Dfir.bench.passes=1 \
-Dfir.bench.prefix=/Users/Simon.Ogorodnik/TestWorkspace/mt-kotlin2 \
-Didea.home.path=/Users/Simon.Ogorodnik/Workspace/KotlinRepo/main/build/ideaHomeForTests \
-Didea.ignore.disabled.plugins=true \
-Didea.is.unit.test=true \
-Didea.use.native.fs.for.win=false \
-Djava.awt.headless=true \
-Djna.nosys=true \
-Djps.kotlin.home=/Users/Simon.Ogorodnik/Workspace/KotlinRepo/main/dist/kotlinc \
-Xmx8192m \
-Xms8192m