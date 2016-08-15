#include "communication.h"

#include <stddef.h>
#include <stdbool.h>
#include <usbd_cdc_vcp.h>

void send_int(int n)
{
    int i = 0;
    char* buffer = (char*) &n;

    VCP_put_char(0xAA);
    for (; i < sizeof(int); ++i) {
        VCP_put_char(buffer[i]);
    }
    VCP_put_char(0xAA);
}

void send_buffer(int size, int pointer)
{
    int i = 0;

    send_int(size);
    VCP_put_char(0xAA);
    char* buffer = (char*) pointer;
    for (; i < size; ++i) {
        VCP_put_char(buffer[i]);
    }
    VCP_put_char(0xAA);
}

int receive_int()
{
    return 0;
}

int receive_buffer()
{
    return 0;
}
