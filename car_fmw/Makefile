BIN_DIR=$(PWD)/bin
SRC_DIR=$(PWD)/src
LIB_DIR=$(SRC_DIR)/lib

LIB_CMSIS_DIR=$(LIB_DIR)/cmsis
LIB_STDPERIPH_DIR=$(LIB_DIR)/stdperiph
LIB_STM32F4D_DIR=$(LIB_DIR)/stm32f4d

LIB_CMSIS_OBJ=$(BIN_DIR)/libcmsis.o
LIB_STDPERIPH_OBJ=$(BIN_DIR)/libstdperiph.o
LIB_STM32F4D_OBJ=$(BIN_DIR)/libstm32f4d.o
CAR_FMW_OBJ=$(BIN_DIR)/car_fmw.o
CAR_FMW_ELF=$(BIN_DIR)/car_fmw.elf
CAR_FMW_BIN=$(BIN_DIR)/car_fmw.bin

CC=arm-none-eabi-gcc
AS=arm-none-eabi-as
LD=arm-none-eabi-ld
OBJ_COPY=arm-none-eabi-objcopy

INCLUDES=-I$(LIB_CMSIS_DIR) \
		 -I$(LIB_STDPERIPH_DIR) \
		 -I$(LIB_STM32F4D_DIR) \
		 -I$(SRC_DIR)
CFLAGS=-g -nostdlib -ffreestanding -O0 \
	   -mcpu=cortex-m3 -mfloat-abi=soft -mthumb
ASMFLAGS=-g -mthumb

LD_ALL_DEPS=$(LD) -r $(filter %.o,$^) -o $@
CC_ALL_DEPS=$(CC) $(CFLAGS) -c $(INCLUDES) $(filter %.c,$^) -o $@
AS_ALL_DEPS=$(AS) $(ASMFLAGS) $(filter %.s,$^) -o $@

dirhs=$(wildcard $(1)/*.h)
dircs=$(wildcard $(1)/*.c)
dircs_to_prefxd_objs=\
	$(patsubst $(1)/%.c,$(BIN_DIR)/$(2)%.o,$(call dircs,$1))

$(CAR_FMW_BIN): $(CAR_FMW_ELF)
	$(OBJ_COPY) -O binary $< $@

$(CAR_FMW_ELF): $(CAR_FMW_OBJ) $(LIB_STM32F4D_OBJ) \
	$(LIB_CMSIS_OBJ) $(LIB_STDPERIPH_OBJ)
	$(CC) $(CFLAGS) $^ -T $(SRC_DIR)/stm32_flash.ld -o $@

$(CAR_FMW_OBJ): $(BIN_DIR) $(call dircs_to_prefxd_objs,$(SRC_DIR))
	$(LD_ALL_DEPS)

$(BIN_DIR):
	mkdir -p $(BIN_DIR)

$(BIN_DIR)/%.o: $(SRC_DIR)/%.c
	$(CC_ALL_DEPS)

clean:
	rm -rf bin/*

include $(LIB_DIR)/**/Makefile

.PHONY: clean
