#include "communication.h"

#include <stddef.h>
#include <stdbool.h>
#include <usbd_cdc_vcp.h>

void send_int(int n)
{
    int i = 0;
    char* buffer = (char*) &n;

    for (; i < sizeof(int); ++i) {
        VCP_put_char(buffer[i]);
    }
}

void send_buffer(int size, int pointer)
{
    int i = 0;

    send_int(size);
    char* buffer = (char*) pointer;
    for (; i < size; ++i) {
        VCP_put_char(buffer[i]);
    }
}

int receive_int()
{
    return 0;
}

int receive_buffer()
{
    return 0;
}
