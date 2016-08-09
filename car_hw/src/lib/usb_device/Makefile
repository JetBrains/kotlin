LIB_USB_DEV_CORE_DIR=$(LIB_USB_DEV_DIR)/core
LIB_USB_DEV_CDC_CLASS_DIR=$(LIB_USB_DEV_DIR)/class/cdc

LIB_USB_DEV_CORE_OBJ=$(BIN_DIR)/libusbdevice_core.o
LIB_USB_DEV_CDC_CLASS_OBJ=$(BIN_DIR)/libusbdevice_clcdc.o

$(LIB_USB_DEV_OBJ): $(LIB_USB_DEV_CORE_OBJ) \
	$(LIB_USB_DEV_CDC_CLASS_OBJ)
	$(LD_ALL_DEPS)

LIB_USB_DEV_CORE_OBJ_PREFIX=libusbdevice_core_

# ===========================================
INCLUDES+=-I$(LIB_USB_DEV_CORE_DIR)/inc

$(LIB_USB_DEV_CORE_OBJ): \
	$(call dircs_to_prefxd_objs,$(LIB_USB_DEV_CORE_DIR)/src,$(LIB_USB_DEV_CORE_OBJ_PREFIX))
	$(LD_ALL_DEPS)

$(BIN_DIR)/$(LIB_USB_DEV_CORE_OBJ_PREFIX)%.o: $(LIB_USB_DEV_CORE_DIR)/src/%.c \
	$(call dirhs,$(LIB_USB_DEV_CORE_DIR)/inc)
	$(CC_ALL_DEPS)

# ===========================================
LIB_USB_DEV_CDC_CLASS_OBJ_PREFIX=libusbdevice_cdc_

INCLUDES+=-I$(LIB_USB_DEV_CDC_CLASS_DIR)/inc

$(LIB_USB_DEV_CDC_CLASS_OBJ): \
	    $(call dircs_to_prefxd_objs,$(LIB_USB_DEV_CDC_CLASS_DIR)/src,$(LIB_USB_DEV_CDC_CLASS_OBJ_PREFIX))
	    $(LD_ALL_DEPS)

$(BIN_DIR)/$(LIB_USB_DEV_CDC_CLASS_OBJ_PREFIX)%.o: $(LIB_USB_DEV_CDC_CLASS_DIR)/src/%.c \
	    $(call dirhs,$(LIB_USB_DEV_CDC_CLASS_DIR)/inc)
	    $(CC_ALL_DEPS)
