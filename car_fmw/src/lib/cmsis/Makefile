LIB_CMSIS_OBJ_PREFIX=libcmsis_

$(LIB_CMSIS_OBJ): \
	$(call dircs_to_prefxd_objs,$(LIB_CMSIS_DIR),$(LIB_CMSIS_OBJ_PREFIX)) \
	$(BIN_DIR)/$(LIB_CMSIS_OBJ_PREFIX)startup_stm32f4xx.o
	$(LD_ALL_DEPS)

$(BIN_DIR)/$(LIB_CMSIS_OBJ_PREFIX)startup_stm32f4xx.o: \
	$(call dirhs, $(LIB_CMSIS_DIR)) \
	$(LIB_CMSIS_DIR)/startup_stm32f4xx.s
	$(AS_ALL_DEPS)

$(BIN_DIR)/$(LIB_CMSIS_OBJ_PREFIX)%.o: $(LIB_CMSIS_DIR)/%.c \
	$(call dirhs, $(LIB_CMSIS_DIR))
	$(CC_ALL_DEPS)
