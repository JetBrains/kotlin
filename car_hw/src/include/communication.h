#pragma once

void clear_buffer();

void send_int(int i);
void send_buffer(int size, int pointer);

int receive_int();
void receive_buffer(int size, int pointer);

void set_state(int i);
int get_state();

