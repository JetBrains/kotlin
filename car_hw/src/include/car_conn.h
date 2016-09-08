#pragma once

#include <stddef.h>
#include <stdint.h>

void car_conn_init(void);
void car_conn_rcv_buf_clear(void);

void car_conn_snd_byte(uint8_t b);
void car_conn_snd_int(int32_t n);
void car_conn_snd_buf(uint32_t size, /*void */int buf);

uint8_t car_conn_rcv_byte(void);
int32_t car_conn_rcv_int(void);
void car_conn_rcv_buf(uint32_t size, /*void **/int buf);
