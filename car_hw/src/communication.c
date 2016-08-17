#include "communication.h"

#include <stddef.h>
#include <stdbool.h>
#include <usbd_cdc_vcp.h>

#include "memory.h"

void clear_buffer()
{
    uint8_t tmp_char;
    while (VCP_get_char(&tmp_char));
}

void send_int(int n)
{
    VCP_put_char(n >> 24);
    VCP_put_char(n >> 16);
    VCP_put_char(n >> 8);
    VCP_put_char(n);
}

void send_buffer(int size, int pointer)
{
    int i = 0;
    char *buffer = (char *) pointer;

    send_int(size);
    for (; i < size; ++i) {
        VCP_put_char(buffer[i]);
    }
}

int receive_int()
{
    int result = 0;
    uint8_t byte = 0;

    while (!VCP_get_char(&byte));
    result += (byte << 24);
    while (!VCP_get_char(&byte));
    result += (byte << 16);
    while (!VCP_get_char(&byte));
    result += (byte << 8);
    while (!VCP_get_char(&byte));
    result += byte;

    return result;
}

void receive_buffer(int size, int pointer)
{
    char* buffer = (char*) pointer;
    int i = 0;

    for (; i < size; ++i) {
        while(!VCP_get_char(&buffer[i]));
    }
}
