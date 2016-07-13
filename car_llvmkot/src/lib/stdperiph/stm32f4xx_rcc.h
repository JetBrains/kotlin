/**
  ******************************************************************************
  * @file    stm32f4xx_rcc.h
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file contains all the functions prototypes for the RCC firmware library.
  ******************************************************************************
  * @attention
  *
  * THE PRESENT FIRMWARE WHICH IS FOR GUIDANCE ONLY AIMS AT PROVIDING CUSTOMERS
  * WITH CODING INFORMATION REGARDING THEIR PRODUCTS IN ORDER FOR THEM TO SAVE
  * TIME. AS A RESULT, STMICROELECTRONICS SHALL NOT BE HELD LIABLE FOR ANY
  * DIRECT, INDIRECT OR CONSEQUENTIAL DAMAGES WITH RESPECT TO ANY CLAIMS ARISING
  * FROM THE CONTENT OF SUCH FIRMWARE AND/OR THE USE MADE BY CUSTOMERS OF THE
  * CODING INFORMATION CONTAINED HEREIN IN CONNECTION WITH THEIR PRODUCTS.
  *
  * <h2><center>&copy; COPYRIGHT 2011 STMicroelectronics</center></h2>
  ******************************************************************************
  */

/* Define to prevent recursive inclusion -------------------------------------*/
#ifndef __STM32F4xx_RCC_H
#define __STM32F4xx_RCC_H

#ifdef __cplusplus
 extern "C" {
#endif

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @addtogroup RCC
  * @{
  */ 

/* Exported types ------------------------------------------------------------*/
typedef struct
{
  uint32_t SYSCLK_Frequency; /*!<  SYSCLK clock frequency expressed in Hz */
  uint32_t HCLK_Frequency;   /*!<  HCLK clock frequency expressed in Hz */
  uint32_t PCLK1_Frequency;  /*!<  PCLK1 clock frequency expressed in Hz */
  uint32_t PCLK2_Frequency;  /*!<  PCLK2 clock frequency expressed in Hz */
}RCC_ClocksTypeDef;

/* Exported constants --------------------------------------------------------*/

/** @defgroup RCC_Exported_Constants
  * @{
  */
  
/** @defgroup RCC_HSE_configuration 
  * @{
  */
#define RCC_HSE_OFF                      ((uint8_t)0x00)
#define RCC_HSE_ON                       ((uint8_t)0x01)
#define RCC_HSE_Bypass                   ((uint8_t)0x05)
#define IS_RCC_HSE(HSE) (((HSE) == RCC_HSE_OFF) || ((HSE) == RCC_HSE_ON) || \
                         ((HSE) == RCC_HSE_Bypass))
/**
  * @}
  */ 
  
/** @defgroup RCC_PLL_Clock_Source 
  * @{
  */
#define RCC_PLLSource_HSI                ((uint32_t)0x00000000)
#define RCC_PLLSource_HSE                ((uint32_t)0x00400000)
#define IS_RCC_PLL_SOURCE(SOURCE) (((SOURCE) == RCC_PLLSource_HSI) || \
                                   ((SOURCE) == RCC_PLLSource_HSE))
#define IS_RCC_PLLM_VALUE(VALUE) ((VALUE) <= 63)
#define IS_RCC_PLLN_VALUE(VALUE) ((192 <= (VALUE)) && ((VALUE) <= 432))
#define IS_RCC_PLLP_VALUE(VALUE) (((VALUE) == 2) || ((VALUE) == 4) || ((VALUE) == 6) || ((VALUE) == 8))
#define IS_RCC_PLLQ_VALUE(VALUE) ((4 <= (VALUE)) && ((VALUE) <= 15))
 
#define IS_RCC_PLLI2SN_VALUE(VALUE) ((192 <= (VALUE)) && ((VALUE) <= 432))
#define IS_RCC_PLLI2SR_VALUE(VALUE) ((2 <= (VALUE)) && ((VALUE) <= 7))   
/**
  * @}
  */ 
  
/** @defgroup RCC_System_Clock_Source 
  * @{
  */
#define RCC_SYSCLKSource_HSI             ((uint32_t)0x00000000)
#define RCC_SYSCLKSource_HSE             ((uint32_t)0x00000001)
#define RCC_SYSCLKSource_PLLCLK          ((uint32_t)0x00000002)
#define IS_RCC_SYSCLK_SOURCE(SOURCE) (((SOURCE) == RCC_SYSCLKSource_HSI) || \
                                      ((SOURCE) == RCC_SYSCLKSource_HSE) || \
                                      ((SOURCE) == RCC_SYSCLKSource_PLLCLK))
/**
  * @}
  */ 
  
/** @defgroup RCC_AHB_Clock_Source
  * @{
  */
#define RCC_SYSCLK_Div1                  ((uint32_t)0x00000000)
#define RCC_SYSCLK_Div2                  ((uint32_t)0x00000080)
#define RCC_SYSCLK_Div4                  ((uint32_t)0x00000090)
#define RCC_SYSCLK_Div8                  ((uint32_t)0x000000A0)
#define RCC_SYSCLK_Div16                 ((uint32_t)0x000000B0)
#define RCC_SYSCLK_Div64                 ((uint32_t)0x000000C0)
#define RCC_SYSCLK_Div128                ((uint32_t)0x000000D0)
#define RCC_SYSCLK_Div256                ((uint32_t)0x000000E0)
#define RCC_SYSCLK_Div512                ((uint32_t)0x000000F0)
#define IS_RCC_HCLK(HCLK) (((HCLK) == RCC_SYSCLK_Div1) || ((HCLK) == RCC_SYSCLK_Div2) || \
                           ((HCLK) == RCC_SYSCLK_Div4) || ((HCLK) == RCC_SYSCLK_Div8) || \
                           ((HCLK) == RCC_SYSCLK_Div16) || ((HCLK) == RCC_SYSCLK_Div64) || \
                           ((HCLK) == RCC_SYSCLK_Div128) || ((HCLK) == RCC_SYSCLK_Div256) || \
                           ((HCLK) == RCC_SYSCLK_Div512))
/**
  * @}
  */ 
  
/** @defgroup RCC_APB1_APB2_Clock_Source
  * @{
  */
#define RCC_HCLK_Div1                    ((uint32_t)0x00000000)
#define RCC_HCLK_Div2                    ((uint32_t)0x00001000)
#define RCC_HCLK_Div4                    ((uint32_t)0x00001400)
#define RCC_HCLK_Div8                    ((uint32_t)0x00001800)
#define RCC_HCLK_Div16                   ((uint32_t)0x00001C00)
#define IS_RCC_PCLK(PCLK) (((PCLK) == RCC_HCLK_Div1) || ((PCLK) == RCC_HCLK_Div2) || \
                           ((PCLK) == RCC_HCLK_Div4) || ((PCLK) == RCC_HCLK_Div8) || \
                           ((PCLK) == RCC_HCLK_Div16))
/**
  * @}
  */ 
  
/** @defgroup RCC_Interrupt_Source 
  * @{
  */
#define RCC_IT_LSIRDY                    ((uint8_t)0x01)
#define RCC_IT_LSERDY                    ((uint8_t)0x02)
#define RCC_IT_HSIRDY                    ((uint8_t)0x04)
#define RCC_IT_HSERDY                    ((uint8_t)0x08)
#define RCC_IT_PLLRDY                    ((uint8_t)0x10)
#define RCC_IT_PLLI2SRDY                 ((uint8_t)0x20)
#define RCC_IT_CSS                       ((uint8_t)0x80)
#define IS_RCC_IT(IT) ((((IT) & (uint8_t)0xC0) == 0x00) && ((IT) != 0x00))
#define IS_RCC_GET_IT(IT) (((IT) == RCC_IT_LSIRDY) || ((IT) == RCC_IT_LSERDY) || \
                           ((IT) == RCC_IT_HSIRDY) || ((IT) == RCC_IT_HSERDY) || \
                           ((IT) == RCC_IT_PLLRDY) || ((IT) == RCC_IT_CSS) || \
                           ((IT) == RCC_IT_PLLI2SRDY))
#define IS_RCC_CLEAR_IT(IT) ((((IT) & (uint8_t)0x40) == 0x00) && ((IT) != 0x00))
/**
  * @}
  */ 
  
/** @defgroup RCC_LSE_Configuration 
  * @{
  */
#define RCC_LSE_OFF                      ((uint8_t)0x00)
#define RCC_LSE_ON                       ((uint8_t)0x01)
#define RCC_LSE_Bypass                   ((uint8_t)0x04)
#define IS_RCC_LSE(LSE) (((LSE) == RCC_LSE_OFF) || ((LSE) == RCC_LSE_ON) || \
                         ((LSE) == RCC_LSE_Bypass))
/**
  * @}
  */ 
  
/** @defgroup RCC_RTC_Clock_Source
  * @{
  */
#define RCC_RTCCLKSource_LSE             ((uint32_t)0x00000100)
#define RCC_RTCCLKSource_LSI             ((uint32_t)0x00000200)
#define RCC_RTCCLKSource_HSE_Div2        ((uint32_t)0x00020300)
#define RCC_RTCCLKSource_HSE_Div3        ((uint32_t)0x00030300)
#define RCC_RTCCLKSource_HSE_Div4        ((uint32_t)0x00040300)
#define RCC_RTCCLKSource_HSE_Div5        ((uint32_t)0x00050300)
#define RCC_RTCCLKSource_HSE_Div6        ((uint32_t)0x00060300)
#define RCC_RTCCLKSource_HSE_Div7        ((uint32_t)0x00070300)
#define RCC_RTCCLKSource_HSE_Div8        ((uint32_t)0x00080300)
#define RCC_RTCCLKSource_HSE_Div9        ((uint32_t)0x00090300)
#define RCC_RTCCLKSource_HSE_Div10       ((uint32_t)0x000A0300)
#define RCC_RTCCLKSource_HSE_Div11       ((uint32_t)0x000B0300)
#define RCC_RTCCLKSource_HSE_Div12       ((uint32_t)0x000C0300)
#define RCC_RTCCLKSource_HSE_Div13       ((uint32_t)0x000D0300)
#define RCC_RTCCLKSource_HSE_Div14       ((uint32_t)0x000E0300)
#define RCC_RTCCLKSource_HSE_Div15       ((uint32_t)0x000F0300)
#define RCC_RTCCLKSource_HSE_Div16       ((uint32_t)0x00100300)
#define RCC_RTCCLKSource_HSE_Div17       ((uint32_t)0x00110300)
#define RCC_RTCCLKSource_HSE_Div18       ((uint32_t)0x00120300)
#define RCC_RTCCLKSource_HSE_Div19       ((uint32_t)0x00130300)
#define RCC_RTCCLKSource_HSE_Div20       ((uint32_t)0x00140300)
#define RCC_RTCCLKSource_HSE_Div21       ((uint32_t)0x00150300)
#define RCC_RTCCLKSource_HSE_Div22       ((uint32_t)0x00160300)
#define RCC_RTCCLKSource_HSE_Div23       ((uint32_t)0x00170300)
#define RCC_RTCCLKSource_HSE_Div24       ((uint32_t)0x00180300)
#define RCC_RTCCLKSource_HSE_Div25       ((uint32_t)0x00190300)
#define RCC_RTCCLKSource_HSE_Div26       ((uint32_t)0x001A0300)
#define RCC_RTCCLKSource_HSE_Div27       ((uint32_t)0x001B0300)
#define RCC_RTCCLKSource_HSE_Div28       ((uint32_t)0x001C0300)
#define RCC_RTCCLKSource_HSE_Div29       ((uint32_t)0x001D0300)
#define RCC_RTCCLKSource_HSE_Div30       ((uint32_t)0x001E0300)
#define RCC_RTCCLKSource_HSE_Div31       ((uint32_t)0x001F0300)
#define IS_RCC_RTCCLK_SOURCE(SOURCE) (((SOURCE) == RCC_RTCCLKSource_LSE) || \
                                      ((SOURCE) == RCC_RTCCLKSource_LSI) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div2) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div3) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div4) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div5) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div6) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div7) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div8) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div9) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div10) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div11) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div12) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div13) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div14) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div15) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div16) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div17) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div18) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div19) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div20) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div21) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div22) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div23) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div24) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div25) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div26) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div27) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div28) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div29) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div30) || \
                                      ((SOURCE) == RCC_RTCCLKSource_HSE_Div31))
/**
  * @}
  */ 
  
/** @defgroup RCC_I2S_Clock_Source
  * @{
  */
#define RCC_I2S2CLKSource_PLLI2S             ((uint8_t)0x00)
#define RCC_I2S2CLKSource_Ext                ((uint8_t)0x01)

#define IS_RCC_I2SCLK_SOURCE(SOURCE) (((SOURCE) == RCC_I2S2CLKSource_PLLI2S) || ((SOURCE) == RCC_I2S2CLKSource_Ext))                                
/**
  * @}
  */ 
  
/** @defgroup RCC_AHB1_Peripherals 
  * @{
  */ 
#define RCC_AHB1Periph_GPIOA             ((uint32_t)0x00000001)
#define RCC_AHB1Periph_GPIOB             ((uint32_t)0x00000002)
#define RCC_AHB1Periph_GPIOC             ((uint32_t)0x00000004)
#define RCC_AHB1Periph_GPIOD             ((uint32_t)0x00000008)
#define RCC_AHB1Periph_GPIOE             ((uint32_t)0x00000010)
#define RCC_AHB1Periph_GPIOF             ((uint32_t)0x00000020)
#define RCC_AHB1Periph_GPIOG             ((uint32_t)0x00000040)
#define RCC_AHB1Periph_GPIOH             ((uint32_t)0x00000080)
#define RCC_AHB1Periph_GPIOI             ((uint32_t)0x00000100)
#define RCC_AHB1Periph_CRC               ((uint32_t)0x00001000)
#define RCC_AHB1Periph_FLITF             ((uint32_t)0x00008000)
#define RCC_AHB1Periph_SRAM1             ((uint32_t)0x00010000)
#define RCC_AHB1Periph_SRAM2             ((uint32_t)0x00020000)
#define RCC_AHB1Periph_BKPSRAM           ((uint32_t)0x00040000)
#define RCC_AHB1Periph_CCMDATARAMEN      ((uint32_t)0x00100000)
#define RCC_AHB1Periph_DMA1              ((uint32_t)0x00200000)
#define RCC_AHB1Periph_DMA2              ((uint32_t)0x00400000)
#define RCC_AHB1Periph_ETH_MAC           ((uint32_t)0x02000000)
#define RCC_AHB1Periph_ETH_MAC_Tx        ((uint32_t)0x04000000)
#define RCC_AHB1Periph_ETH_MAC_Rx        ((uint32_t)0x08000000)
#define RCC_AHB1Periph_ETH_MAC_PTP       ((uint32_t)0x10000000)
#define RCC_AHB1Periph_OTG_HS            ((uint32_t)0x20000000)
#define RCC_AHB1Periph_OTG_HS_ULPI       ((uint32_t)0x40000000)
#define IS_RCC_AHB1_CLOCK_PERIPH(PERIPH) ((((PERIPH) & 0x818BEE00) == 0x00) && ((PERIPH) != 0x00))
#define IS_RCC_AHB1_RESET_PERIPH(PERIPH) ((((PERIPH) & 0xDD9FEE00) == 0x00) && ((PERIPH) != 0x00))
#define IS_RCC_AHB1_LPMODE_PERIPH(PERIPH) ((((PERIPH) & 0x81986E00) == 0x00) && ((PERIPH) != 0x00))
/**
  * @}
  */ 
  
/** @defgroup RCC_AHB2_Peripherals 
  * @{
  */  
#define RCC_AHB2Periph_DCMI              ((uint32_t)0x00000001)
#define RCC_AHB2Periph_CRYP              ((uint32_t)0x00000010)
#define RCC_AHB2Periph_HASH              ((uint32_t)0x00000020)
#define RCC_AHB2Periph_RNG               ((uint32_t)0x00000040)
#define RCC_AHB2Periph_OTG_FS            ((uint32_t)0x00000080)
#define IS_RCC_AHB2_PERIPH(PERIPH) ((((PERIPH) & 0xFFFFFF0E) == 0x00) && ((PERIPH) != 0x00))
/**
  * @}
  */ 
  
/** @defgroup RCC_AHB3_Peripherals 
  * @{
  */ 
#define RCC_AHB3Periph_FSMC               ((uint32_t)0x00000001)
#define IS_RCC_AHB3_PERIPH(PERIPH) ((((PERIPH) & 0xFFFFFFFE) == 0x00) && ((PERIPH) != 0x00))
/**
  * @}
  */ 
  
/** @defgroup RCC_APB1_Peripherals 
  * @{
  */ 
#define RCC_APB1Periph_TIM2              ((uint32_t)0x00000001)
#define RCC_APB1Periph_TIM3              ((uint32_t)0x00000002)
#define RCC_APB1Periph_TIM4              ((uint32_t)0x00000004)
#define RCC_APB1Periph_TIM5              ((uint32_t)0x00000008)
#define RCC_APB1Periph_TIM6              ((uint32_t)0x00000010)
#define RCC_APB1Periph_TIM7              ((uint32_t)0x00000020)
#define RCC_APB1Periph_TIM12             ((uint32_t)0x00000040)
#define RCC_APB1Periph_TIM13             ((uint32_t)0x00000080)
#define RCC_APB1Periph_TIM14             ((uint32_t)0x00000100)
#define RCC_APB1Periph_WWDG              ((uint32_t)0x00000800)
#define RCC_APB1Periph_SPI2              ((uint32_t)0x00004000)
#define RCC_APB1Periph_SPI3              ((uint32_t)0x00008000)
#define RCC_APB1Periph_USART2            ((uint32_t)0x00020000)
#define RCC_APB1Periph_USART3            ((uint32_t)0x00040000)
#define RCC_APB1Periph_UART4             ((uint32_t)0x00080000)
#define RCC_APB1Periph_UART5             ((uint32_t)0x00100000)
#define RCC_APB1Periph_I2C1              ((uint32_t)0x00200000)
#define RCC_APB1Periph_I2C2              ((uint32_t)0x00400000)
#define RCC_APB1Periph_I2C3              ((uint32_t)0x00800000)
#define RCC_APB1Periph_CAN1              ((uint32_t)0x02000000)
#define RCC_APB1Periph_CAN2              ((uint32_t)0x04000000)
#define RCC_APB1Periph_PWR               ((uint32_t)0x10000000)
#define RCC_APB1Periph_DAC               ((uint32_t)0x20000000)
#define IS_RCC_APB1_PERIPH(PERIPH) ((((PERIPH) & 0xC9013600) == 0x00) && ((PERIPH) != 0x00))
/**
  * @}
  */ 
  
/** @defgroup RCC_APB2_Peripherals 
  * @{
  */ 
#define RCC_APB2Periph_TIM1              ((uint32_t)0x00000001)
#define RCC_APB2Periph_TIM8              ((uint32_t)0x00000002)
#define RCC_APB2Periph_USART1            ((uint32_t)0x00000010)
#define RCC_APB2Periph_USART6            ((uint32_t)0x00000020)
#define RCC_APB2Periph_ADC               ((uint32_t)0x00000100)
#define RCC_APB2Periph_ADC1              ((uint32_t)0x00000100)
#define RCC_APB2Periph_ADC2              ((uint32_t)0x00000200)
#define RCC_APB2Periph_ADC3              ((uint32_t)0x00000400)
#define RCC_APB2Periph_SDIO              ((uint32_t)0x00000800)
#define RCC_APB2Periph_SPI1              ((uint32_t)0x00001000)
#define RCC_APB2Periph_SYSCFG            ((uint32_t)0x00004000)
#define RCC_APB2Periph_TIM9              ((uint32_t)0x00010000)
#define RCC_APB2Periph_TIM10             ((uint32_t)0x00020000)
#define RCC_APB2Periph_TIM11             ((uint32_t)0x00040000)
#define IS_RCC_APB2_PERIPH(PERIPH) ((((PERIPH) & 0xFFF8A0CC) == 0x00) && ((PERIPH) != 0x00))
#define IS_RCC_APB2_RESET_PERIPH(PERIPH) ((((PERIPH) & 0xFFF8A6CC) == 0x00) && ((PERIPH) != 0x00))
/**
  * @}
  */ 
  
/** @defgroup RCC_MCO1_Clock_Source_Prescaler
  * @{
  */
#define RCC_MCO1Source_HSI               ((uint32_t)0x00000000)
#define RCC_MCO1Source_LSE               ((uint32_t)0x00200000)
#define RCC_MCO1Source_HSE               ((uint32_t)0x00400000)
#define RCC_MCO1Source_PLLCLK            ((uint32_t)0x00600000)
#define RCC_MCO1Div_1                    ((uint32_t)0x00000000)
#define RCC_MCO1Div_2                    ((uint32_t)0x04000000)
#define RCC_MCO1Div_3                    ((uint32_t)0x05000000)
#define RCC_MCO1Div_4                    ((uint32_t)0x06000000)
#define RCC_MCO1Div_5                    ((uint32_t)0x07000000)
#define IS_RCC_MCO1SOURCE(SOURCE) (((SOURCE) == RCC_MCO1Source_HSI) || ((SOURCE) == RCC_MCO1Source_LSE) || \
                                   ((SOURCE) == RCC_MCO1Source_HSE) || ((SOURCE) == RCC_MCO1Source_PLLCLK))
                                   
#define IS_RCC_MCO1DIV(DIV) (((DIV) == RCC_MCO1Div_1) || ((DIV) == RCC_MCO1Div_2) || \
                             ((DIV) == RCC_MCO1Div_3) || ((DIV) == RCC_MCO1Div_4) || \
                             ((DIV) == RCC_MCO1Div_5)) 
/**
  * @}
  */ 
  
/** @defgroup RCC_MCO2_Clock_Source_Prescaler
  * @{
  */
#define RCC_MCO2Source_SYSCLK            ((uint32_t)0x00000000)
#define RCC_MCO2Source_PLLI2SCLK         ((uint32_t)0x40000000)
#define RCC_MCO2Source_HSE               ((uint32_t)0x80000000)
#define RCC_MCO2Source_PLLCLK            ((uint32_t)0xC0000000)
#define RCC_MCO2Div_1                    ((uint32_t)0x00000000)
#define RCC_MCO2Div_2                    ((uint32_t)0x20000000)
#define RCC_MCO2Div_3                    ((uint32_t)0x28000000)
#define RCC_MCO2Div_4                    ((uint32_t)0x30000000)
#define RCC_MCO2Div_5                    ((uint32_t)0x38000000)
#define IS_RCC_MCO2SOURCE(SOURCE) (((SOURCE) == RCC_MCO2Source_SYSCLK) || ((SOURCE) == RCC_MCO2Source_PLLI2SCLK)|| \
                                   ((SOURCE) == RCC_MCO2Source_HSE) || ((SOURCE) == RCC_MCO2Source_PLLCLK))
                                   
#define IS_RCC_MCO2DIV(DIV) (((DIV) == RCC_MCO2Div_1) || ((DIV) == RCC_MCO2Div_2) || \
                             ((DIV) == RCC_MCO2Div_3) || ((DIV) == RCC_MCO2Div_4) || \
                             ((DIV) == RCC_MCO2Div_5))                             
/**
  * @}
  */ 
  
/** @defgroup RCC_Flag 
  * @{
  */
#define RCC_FLAG_HSIRDY                  ((uint8_t)0x21)
#define RCC_FLAG_HSERDY                  ((uint8_t)0x31)
#define RCC_FLAG_PLLRDY                  ((uint8_t)0x39)
#define RCC_FLAG_PLLI2SRDY               ((uint8_t)0x3B)
#define RCC_FLAG_LSERDY                  ((uint8_t)0x41)
#define RCC_FLAG_LSIRDY                  ((uint8_t)0x61)
#define RCC_FLAG_BORRST                  ((uint8_t)0x79)
#define RCC_FLAG_PINRST                  ((uint8_t)0x7A)
#define RCC_FLAG_PORRST                  ((uint8_t)0x7B)
#define RCC_FLAG_SFTRST                  ((uint8_t)0x7C)
#define RCC_FLAG_IWDGRST                 ((uint8_t)0x7D)
#define RCC_FLAG_WWDGRST                 ((uint8_t)0x7E)
#define RCC_FLAG_LPWRRST                 ((uint8_t)0x7F)
#define IS_RCC_FLAG(FLAG) (((FLAG) == RCC_FLAG_HSIRDY) || ((FLAG) == RCC_FLAG_HSERDY) || \
                           ((FLAG) == RCC_FLAG_PLLRDY) || ((FLAG) == RCC_FLAG_LSERDY) || \
                           ((FLAG) == RCC_FLAG_LSIRDY) || ((FLAG) == RCC_FLAG_BORRST) || \
                           ((FLAG) == RCC_FLAG_PINRST) || ((FLAG) == RCC_FLAG_PORRST) || \
                           ((FLAG) == RCC_FLAG_SFTRST) || ((FLAG) == RCC_FLAG_IWDGRST)|| \
                           ((FLAG) == RCC_FLAG_WWDGRST)|| ((FLAG) == RCC_FLAG_LPWRRST)|| \
                           ((FLAG) == RCC_FLAG_PLLI2SRDY))
#define IS_RCC_CALIBRATION_VALUE(VALUE) ((VALUE) <= 0x1F)
/**
  * @}
  */ 

/**
  * @}
  */ 

/* Exported macro ------------------------------------------------------------*/
/* Exported functions --------------------------------------------------------*/ 

/* Function used to set the RCC clock configuration to the default reset state */
void RCC_DeInit(void);

/* Internal/external clocks, PLL, CSS and MCO configuration functions *********/
void RCC_HSEConfig(uint8_t RCC_HSE);
ErrorStatus RCC_WaitForHSEStartUp(void);
void RCC_AdjustHSICalibrationValue(uint8_t HSICalibrationValue);
void RCC_HSICmd(FunctionalState NewState);
void RCC_LSEConfig(uint8_t RCC_LSE);
void RCC_LSICmd(FunctionalState NewState);

void RCC_PLLConfig(uint32_t RCC_PLLSource, uint32_t PLLM, uint32_t PLLN, uint32_t PLLP, uint32_t PLLQ);
void RCC_PLLCmd(FunctionalState NewState);
void RCC_PLLI2SConfig(uint32_t PLLI2SN, uint32_t PLLI2SR);
void RCC_PLLI2SCmd(FunctionalState NewState);

void RCC_ClockSecuritySystemCmd(FunctionalState NewState);
void RCC_MCO1Config(uint32_t RCC_MCO1Source, uint32_t RCC_MCO1Div);
void RCC_MCO2Config(uint32_t RCC_MCO2Source, uint32_t RCC_MCO2Div);

/* System, AHB and APB busses clocks configuration functions ******************/
void RCC_SYSCLKConfig(uint32_t RCC_SYSCLKSource);
uint8_t RCC_GetSYSCLKSource(void);
void RCC_HCLKConfig(uint32_t RCC_SYSCLK);
void RCC_PCLK1Config(uint32_t RCC_HCLK);
void RCC_PCLK2Config(uint32_t RCC_HCLK);
void RCC_GetClocksFreq(RCC_ClocksTypeDef* RCC_Clocks);

/* Peripheral clocks configuration functions **********************************/
void RCC_RTCCLKConfig(uint32_t RCC_RTCCLKSource);
void RCC_RTCCLKCmd(FunctionalState NewState);
void RCC_BackupResetCmd(FunctionalState NewState);
void RCC_I2SCLKConfig(uint32_t RCC_I2SCLKSource); 

void RCC_AHB1PeriphClockCmd(uint32_t RCC_AHB1Periph, FunctionalState NewState);
void RCC_AHB2PeriphClockCmd(uint32_t RCC_AHB2Periph, FunctionalState NewState);
void RCC_AHB3PeriphClockCmd(uint32_t RCC_AHB3Periph, FunctionalState NewState);
void RCC_APB1PeriphClockCmd(uint32_t RCC_APB1Periph, FunctionalState NewState);
void RCC_APB2PeriphClockCmd(uint32_t RCC_APB2Periph, FunctionalState NewState);

void RCC_AHB1PeriphResetCmd(uint32_t RCC_AHB1Periph, FunctionalState NewState);
void RCC_AHB2PeriphResetCmd(uint32_t RCC_AHB2Periph, FunctionalState NewState);
void RCC_AHB3PeriphResetCmd(uint32_t RCC_AHB3Periph, FunctionalState NewState);
void RCC_APB1PeriphResetCmd(uint32_t RCC_APB1Periph, FunctionalState NewState);
void RCC_APB2PeriphResetCmd(uint32_t RCC_APB2Periph, FunctionalState NewState);

void RCC_AHB1PeriphClockLPModeCmd(uint32_t RCC_AHB1Periph, FunctionalState NewState);
void RCC_AHB2PeriphClockLPModeCmd(uint32_t RCC_AHB2Periph, FunctionalState NewState);
void RCC_AHB3PeriphClockLPModeCmd(uint32_t RCC_AHB3Periph, FunctionalState NewState);
void RCC_APB1PeriphClockLPModeCmd(uint32_t RCC_APB1Periph, FunctionalState NewState);
void RCC_APB2PeriphClockLPModeCmd(uint32_t RCC_APB2Periph, FunctionalState NewState);

/* Interrupts and flags management functions **********************************/
void RCC_ITConfig(uint8_t RCC_IT, FunctionalState NewState);
FlagStatus RCC_GetFlagStatus(uint8_t RCC_FLAG);
void RCC_ClearFlag(void);
ITStatus RCC_GetITStatus(uint8_t RCC_IT);
void RCC_ClearITPendingBit(uint8_t RCC_IT);

#ifdef __cplusplus
}
#endif

#endif /* __STM32F4xx_RCC_H */

/**
  * @}
  */ 

/**
  * @}
  */ 

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
