DIRECTORY = "src/test/kotlin/tests"
OUTPUT = $(shell ls ${DIRECTORY}/*/*.txt)
OBJECTS = $(OUTPUT:.txt=.out)
green = '\033[0;32m'
red = '\033[0;31m'
lightRed = '\033[1;31m'
orange = '\033[0;33m'
nc = '\033[0m'
KOTSTD='../kotstd'
SOURCELIB=build/sources

stdlib:
	cd $(KOTSTD) && make clean && make

%.out: stdlib %.txt
	@./run_one_test.sh $(@:.out=.txt)

tests: stdlib $(OBJECTS)
	@date


build:
	./gradlew jar

all: build tests;

clean:
	./gradlew clean

compile: build	
	mkdir -p $(SOURCELIB)
	rm -f $(SOURCELIB)/linked.ll
	rm -f $(SOURCELIB)/linked.ll
	java -jar build/libs/translator-1.0.jar -I $(KOTSTD)/kt $(files) -o $(SOURCELIB)/build.ll
	llvm-link-3.6 -S $(KOTSTD)/build/stdlib_x86.ll $(SOURCELIB)/build.ll >> $(SOURCELIB)/linked.ll
	clang-3.6 $(SOURCELIB)/linked.ll -o $(output)
	rm $(SOURCELIB)/linked.ll
	rm $(SOURCELIB)/build.ll
	

.DEFAULT_GOAL := all
.FORCE: build, all
.PHONY: tests, stdlib, build, all, compile
