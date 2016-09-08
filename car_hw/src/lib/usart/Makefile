LIB_USART_OBJ_PREFIX=libusart_

$(LIB_USART_OBJ): \
	    $(call dircs_to_prefxd_objs,$(LIB_USART_DIR),$(LIB_USART_OBJ_PREFIX))
	    $(LD_ALL_DEPS)

$(BIN_DIR)/$(LIB_USART_OBJ_PREFIX)%.o: $(LIB_USART_DIR)/%.c \
	    $(call dirhs,$(LIB_USART_DIR))
	    $(CC_ALL_DEPS)
