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
    int i = 0;
    char *buffer = (char *) &n;

    for (; i < sizeof(int); ++i) {
        VCP_put_char(buffer[i]);
    }
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
    int i = 0;
    char buffer[sizeof(int)];

    while (i < 4) {
        while (!VCP_get_char(&buffer[i]));
        i++;
    }

    return *((int*) buffer);
}

void receive_buffer(int size, int pointer)
{
    char* buffer = (char*) pointer;
    int i = 0;

    for (; i < size; ++i) {
        while(!VCP_get_char(&buffer[i]));
    }

    return buffer;
}
