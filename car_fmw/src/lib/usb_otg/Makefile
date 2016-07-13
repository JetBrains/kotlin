LIB_USB_OTG_OBJ_PREFIX=libusbotg_

INCLUDES+=-I$(LIB_USB_OTG_DIR)/inc

$(LIB_USB_OTG_OBJ): \
	    $(call dircs_to_prefxd_objs,$(LIB_USB_OTG_DIR)/src,$(LIB_USB_OTG_OBJ_PREFIX))
	    $(LD_ALL_DEPS)

$(BIN_DIR)/$(LIB_USB_OTG_OBJ_PREFIX)%.o: $(LIB_USB_OTG_DIR)/src/%.c \
	    $(call dirhs,$(LIB_USB_OTG_DIR)/inc)
	    $(CC_ALL_DEPS)
