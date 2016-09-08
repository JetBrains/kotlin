#include "car_conn.h"

#include <usbd_cdc_vcp.h>

void car_conn_init(void)
{
    VCP_init();
    car_conn_rcv_buf_clear();
}

void car_conn_rcv_buf_clear(void)
{
    uint8_t tmp_char;
    while (VCP_get_char(&tmp_char));
}

void car_conn_snd_byte(uint8_t b)
{
    VCP_put_char(b);
}

void car_conn_snd_int(int32_t n)
{
    VCP_put_char(n >> 24);
    VCP_put_char(n >> 16);
    VCP_put_char(n >> 8);
    VCP_put_char(n);
}

void car_conn_snd_buf(uint32_t size, int buf)
{
    size_t i = 0;
    uint8_t *cbuf = (uint8_t *)buf;

    for (; i < size; ++i)
        car_conn_snd_byte(cbuf[i]);
}

uint8_t car_conn_rcv_byte(void)
{
    uint8_t byte = 0;
    while (!VCP_get_char(&byte));
    return byte;
}

int32_t car_conn_rcv_int(void)
{
    int32_t result = 0;

    result += (car_conn_rcv_byte() << 24);
    result += (car_conn_rcv_byte() << 16);
    result += (car_conn_rcv_byte() << 8);
    result += car_conn_rcv_byte(); 
    return result;
}

void car_conn_rcv_buf(uint32_t size, int buf)
{
    uint8_t* cbuf = (uint8_t *)buf;
    size_t i = 0;

    for (; i < size; ++i)
        cbuf[i] = car_conn_rcv_byte();
}
