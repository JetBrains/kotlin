#pragma once

#include <stdint.h>
#include <stddef.h>

// Warning: USART1 doesn't pass uarts_test
enum usart_id_t {
    USART1_ID = 0, // PA9: TX, PA10: RX
    USART2_ID = 1, // PA2: TX, PA3: RX
    USART3_ID = 2, // PC10: TX, PC11: RX
};

void usart_init(enum usart_id_t usart_id);
void usart_send_data(enum usart_id_t usart_id, uint8_t *data, size_t size);
void usart_rcv_data(enum usart_id_t usart_id, uint8_t *data, size_t size);

#define MT_USART_BAUDRATE 9600
