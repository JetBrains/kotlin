
LLVM_VERSION=3.6

CLANG=clang-mp-${LLVM_VERSION}
LLVM_AS=llvm-as-mp-${LLVM_VERSION}
LLVM_LC=llc-mp-${LLVM_VERSION}
LLVM_DWARF_DUMP=llvm-dwarfdump-mp-${LLVM_VERSION}

%.o:%.c
%.ll:%.c
	${CLANG} -emit-llvm -g -S -o $@ $<

%.bc:%.ll
	${LLVM_AS} -o $@ $<

%.s:%.bc
	${LLVM_LC} -o $@ $<

%.asc:%.o
	${LLVM_DWARF_DUMP} $< > $@

.PHONY: clean all

all:main.asc classfields_1.asc

clean:
	${RM} *.o *.asc *.bc *.s main.ll

