LIB_USB_VCP_OBJ_PREFIX=libusbvcp_

$(LIB_USB_VCP_OBJ): \
	    $(call dircs_to_prefxd_objs,$(LIB_USB_VCP_DIR),$(LIB_USB_VCP_OBJ_PREFIX))
	    $(LD_ALL_DEPS)

$(BIN_DIR)/$(LIB_USB_VCP_OBJ_PREFIX)%.o: $(LIB_USB_VCP_DIR)/%.c \
	    $(call dirhs,$(LIB_USB_VCP_DIR))
	    $(CC_ALL_DEPS)
