SRC_DIR=src
BIN_DIR=bin

CAR_FMW_OBJ=$(BIN_DIR)/car_fmw.o
CAR_FMW_ELF=$(BIN_DIR)/car_fmw.elf
CAR_FMW_BIN=$(BIN_DIR)/car_fmw.bin

LIB_CARHW_DIR=../car_hw/
LIB_CARHW_OBJ=$(LIB_CARHW_DIR)/bin/car_hw.o
LIB_CARHW_LIB_DIR=$(LIB_CARHW_DIR)/src/lib/

CC=arm-none-eabi-gcc
AS=arm-none-eabi-as
LD=arm-none-eabi-ld
OBJ_COPY=arm-none-eabi-objcopy

INCLUDES=-I$(LIB_CARHW_LIB_DIR)/cmsis \
		 -I$(LIB_CARHW_LIB_DIR)/stdperiph \
		 -I$(LIB_CARHW_LIB_DIR)/stm32f4d \
		 -I$(LIB_CARHW_LIB_DIR)/usb_device \
		 -I$(LIB_CARHW_LIB_DIR)/usb_otg \
		 -I$(LIB_CARHW_LIB_DIR)/usb_vcp \
		 -I$(LIB_CARHW_LIB_DIR)/usart \
		 -I$(LIB_CARHW_DIR)/src/include \
		 -I$(SRC_DIR)

DEFINES=-DUSE_STM32_DISCOVERY=1 -DUSE_STDPERIPH_DRIVER=1 -DSTM32F4XX=1 -DHSE_VALUE=8000000
CFLAGS=-g -nostdlib -ffreestanding -O0 \
       -mcpu=cortex-m3 -mfloat-abi=soft -mthumb \
       $(DEFINES)
ASMFLAGS=-g -mthumb

LD_ALL_DEPS=$(LD) -r $(filter %.o,$^) -o $@
CC_ALL_DEPS=$(CC) $(CFLAGS) -c $(INCLUDES) $(filter %.c,$^) -o $@
AS_ALL_DEPS=$(AS) $(ASMFLAGS) $(filter %.s,$^) -o $@

dirhs=$(wildcard $(1)/*.h)
dircs=$(wildcard $(1)/*.c)
dircs_to_prefxd_objs=\
	$(patsubst $(1)/%.c,$(BIN_DIR)/$(2)%.o,$(call dircs,$1))

CARFMW_OBJ_PREFIX=carfmw_

$(CAR_FMW_BIN): $(CAR_FMW_ELF) $(BIN_DIR)
	$(OBJ_COPY) -O binary $< $@

$(CAR_FMW_ELF): $(CAR_FMW_OBJ) $(LIB_CARHW_OBJ)
	$(CC) $(CFLAGS) $^ -T $(SRC_DIR)/stm32_flash.ld -o $@

$(CAR_FMW_OBJ): \
	$(call dircs_to_prefxd_objs,$(SRC_DIR),$(CARFMW_OBJ_PREFIX))
	$(LD_ALL_DEPS)

$(BIN_DIR)/$(CARFMW_OBJ_PREFIX)%.o: $(SRC_DIR)/%.c \
	$(call dirhs,$(SRC_DIR))
	$(CC_ALL_DEPS)

$(LIB_CARHW_OBJ):
	make -C $(LIB_CARHW_DIR)

$(BIN_DIR):
	mkdir -p $(BIN_DIR)

clean:
	rm -rf bin/*

tags:
	ctags -R * $(LIB_CARHW_DIR)

.PHONY: clean tags
