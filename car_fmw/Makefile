BIN_DIR=$(PWD)/bin
SRC_DIR=$(PWD)/src
LIB_DIR=$(PWD)/lib
TRANSLATOR_DIR=$(PWD)/../translator
KOTSTD_DIR=$(PWD)/../kotstd
CAR_HW_DIR=$(PWD)/../car_hw

OS_ARCH=linux-x86_64

CAR_FMW_OBJ=$(BIN_DIR)/car_fmw.o
CAR_FMW_ELF=$(BIN_DIR)/car_fmw.elf
CAR_FMW_BIN=$(BIN_DIR)/car_fmw.bin
ST_DIR=$(PWD)/../mcu_utils/$(OS_ARCH)

LIB_KOT=$(BIN_DIR)/kotlib.o
LIB_KOTSTD=$(KOTSTD_DIR)/build/stdlib_arm.ll
LIB_CAR_HW=$(CAR_HW_DIR)/bin/car_hw.o

CC=arm-none-eabi-gcc
AS=arm-none-eabi-as
KT=$(TRANSLATOR_DIR)/build/libs/translator-1.0.jar
LD=arm-none-eabi-ld
OBJ_COPY=arm-none-eabi-objcopy
GDB=arm-none-eabi-gdb
PROTOC=$(PWD)/../proto/compiler/build/protoc

LL=llc-3.6
LLINK=llvm-link-3.6

KTFLAGS=-I $(KOTSTD_DIR)/include --arm
LLINK_FLAGS=-S
ASMFLAGS=-g -mthumb
LLFLAGS=-O0

DEFINES=-DUSE_STM32_DISCOVERY=1 -DUSE_STDPERIPH_DRIVER=1 -DSTM32F4XX=1 -DHSE_VALUE=8000000
CFLAGS=-g -nostdlib -ffreestanding -O0 \
       -mcpu=cortex-m3 -mfloat-abi=soft -mthumb \
       $(DEFINES)

LD_ALL_DEPS=$(LD) -r $(filter %.o,$^) -o $@
AS_ALL_DEPS=$(AS) $(ASMFLAGS) $(filter %.s,$^) -o $@
LL_ALL_DEPS=$(LL) $(LLFLAGS) $(filter %.ll, $^) -o $@
KT_ALL_DEPS=java -jar $(KT) $(KTFLAGS) -o $@ $(filter %.kt, $^)
LLINK_ALL_DEPS=$(LLINK) $(LLINK_FLAGS) $(filter %.ll,$^) > $@

all: $(KT) $(CAR_FMW_BIN)

$(CAR_FMW_BIN): $(CAR_FMW_ELF) $(BIN_DIR)
	$(OBJ_COPY) -O binary $< $@

$(CAR_FMW_ELF): $(LIB_CAR_HW) $(LIB_KOT)
	$(CC) -Os $(CFLAGS) $^ -T stm32_flash.ld -o $@

$(LIB_CAR_HW):
	cd $(CAR_HW_DIR) && make

$(BIN_DIR):
	mkdir -p $(BIN_DIR)

$(LIB_KOT): $(BIN_DIR)/kotlib.s
	$(AS_ALL_DEPS)

$(BIN_DIR)/kotlib.s: $(BIN_DIR)/kotlib.ll
	$(LL_ALL_DEPS)

$(BIN_DIR)/kotlib.ll: $(LIB_DIR)/app.ll $(LIB_KOTSTD)
	$(LLINK_ALL_DEPS)

$(LIB_KOTSTD):
	cd $(KOTSTD_DIR) && make

$(LIB_DIR)/app.ll: $(SRC_DIR)/**/*.kt $(SRC_DIR)/*.kt
	$(KT_ALL_DEPS)

$(KT):
	cd $(TRANSLATOR_DIR) && ./gradlew jar

proto:
	$(PROTOC) -I$(SRC_DIR)/proto --kotlin_out=$(SRC_DIR)/proto_src $(SRC_DIR)/proto/*.proto

flash: $(CAR_FMW_BIN)
	$(ST_DIR)/st-flash write $(CAR_FMW_BIN) 0x8000000

debug:
	-killall st-util
	(setsid $(ST_DIR)/st-util 2>/dev/null 1>&2)&
	$(GDB) $(CAR_FMW_ELF) -tui -x $(ST_DIR)/../gdb.cmds

clean:
	rm -rf bin/*
	rm -rf lib/*
	cd $(CAR_HW_DIR) && make clean
	cd $(KOTSTD_DIR) && make clean

.PHONY: clean debug flash
