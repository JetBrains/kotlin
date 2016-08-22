BIN_DIR=bin
SRC_DIR=src
LIB_DIR=$(SRC_DIR)/lib
OS_ARCH=linux-arm

LIB_CMSIS_DIR=$(LIB_DIR)/cmsis
LIB_STDPERIPH_DIR=$(LIB_DIR)/stdperiph
LIB_STM32F4D_DIR=$(LIB_DIR)/stm32f4d
LIB_USB_DEV_DIR=$(LIB_DIR)/usb_device
LIB_USB_OTG_DIR=$(LIB_DIR)/usb_otg
LIB_USB_VCP_DIR=$(LIB_DIR)/usb_vcp
LIB_USART_DIR=$(LIB_DIR)/usart

LIB_CMSIS_OBJ=$(BIN_DIR)/libcmsis.o
LIB_STDPERIPH_OBJ=$(BIN_DIR)/libstdperiph.o
LIB_STM32F4D_OBJ=$(BIN_DIR)/libstm32f4d.o
LIB_USB_DEV_OBJ=$(BIN_DIR)/libusbdevice.o
LIB_USB_OTG_OBJ=$(BIN_DIR)/libusbotg.o
LIB_USB_VCP_OBJ=$(BIN_DIR)/libusbvcp.o
LIB_USART_OBJ=$(BIN_DIR)/libusart.o

CARHW_OBJ=$(BIN_DIR)/car_hw.o

CC=arm-none-eabi-gcc
AS=arm-none-eabi-as
LD=arm-none-eabi-ld
OBJ_COPY=arm-none-eabi-objcopy
GDB=arm-none-eabi-gdb

USB_CONF_DIR=$(LIB_DIR)/usb_conf
INCLUDES=-I$(LIB_CMSIS_DIR) \
		 -I$(LIB_STDPERIPH_DIR) \
		 -I$(LIB_STM32F4D_DIR) \
		 -I$(LIB_USB_DEV_DIR) \
		 -I$(LIB_USB_OTG_DIR) \
		 -I$(LIB_USB_VCP_DIR) \
		 -I$(LIB_USART_DIR) \
		 -I$(USB_CONF_DIR) \
		 -I$(SRC_DIR) \
		 -I$(SRC_DIR)/include

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

CAR_OBJ=$(BIN_DIR)/car.o
CAR_OBJ_PREFIX=car_

$(CARHW_OBJ): $(CAR_OBJ) $(LIB_STM32F4D_OBJ) \
		$(LIB_CMSIS_OBJ) $(LIB_STDPERIPH_OBJ) \
		$(LIB_USB_DEV_OBJ) $(LIB_USB_OTG_OBJ) \
		$(LIB_USB_VCP_OBJ) $(LIB_USART_OBJ)
	$(LD) -r $^ -o $@

$(CAR_OBJ): \
		$(call dircs_to_prefxd_objs,$(SRC_DIR),$(CAR_OBJ_PREFIX))
	$(LD_ALL_DEPS)

$(BIN_DIR)/$(CAR_OBJ_PREFIX)%.o: $(SRC_DIR)/%.c \
		$(call dirhs,$(SRC_DIR)/include)
	$(CC_ALL_DEPS)

clean:
	rm -rf $(BIN_DIR)/*

tags:
	ctags -R *

include $(LIB_DIR)/**/Makefile

.PHONY: tags clean
