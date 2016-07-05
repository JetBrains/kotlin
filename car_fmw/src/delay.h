#pragma once

#include "stm32f4_discovery.h"
#include "stm32f4xx_conf.h"

/**
  * @brief  Delay Function.
  * @param  nCount:specifies the Delay time length.
  * @retval None
  */
inline void Delay(__IO uint32_t nCount)
{
  while(nCount--)
  {
  }
}
