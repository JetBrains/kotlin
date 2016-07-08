#pragma once

inline void wait(uint32_t loops)
{
    while(--loops);
}
