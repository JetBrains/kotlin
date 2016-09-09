
LLVM_VERSION=3.6

CLANG=clang-mp-${LLVM_VERSION}
LLVM_AS=llvm-as-mp-${LLVM_VERSION}
LLVM_LC=llc-mp-${LLVM_VERSION}
LLVM_DWARF_DUMP=llvm-dwarfdump-mp-${LLVM_VERSION}
TRANSLATOR_JAR=../translator/build/libs/translator-1.0.jar

# Here used manually edited ll from kt
#%.ll:%.kt
#	$(JAVA) -cp ${TRANSLATO_JAR} $< > $@
%.o:%.c
%.ll:%.c
	${CLANG} -emit-llvm -g -S -o $@ $<

%.bc:%.ll
	${LLVM_AS} -o $@ $<

%.s:%.bc
	${LLVM_LC} -o $@ $<

%.asc:%.o
	${LLVM_DWARF_DUMP} $< > $@

all:main.asc classfields_1.asc 2-functions.asc

clean:
	${RM} *.o *.asc *.bc *.s main.ll

.PHONY: clean all
.PRECIOUS:%.ll %.bc %.s %.o
