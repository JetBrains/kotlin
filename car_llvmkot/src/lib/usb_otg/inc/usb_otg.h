/**
  ******************************************************************************
  * @file    usb_otg.h
  * @author  MCD Application Team
  * @version V2.0.0
  * @date    22-July-2011
  * @brief   OTG Core Header
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
#ifndef __USB_OTG__
#define __USB_OTG__


/** @addtogroup USB_OTG_DRIVER
  * @{
  */
  
/** @defgroup USB_OTG
  * @brief This file is the 
  * @{
  */ 


/** @defgroup USB_OTG_Exported_Defines
  * @{
  */ 


void USB_OTG_InitiateSRP(void);
void USB_OTG_InitiateHNP(uint8_t state , uint8_t mode);
void USB_OTG_Switchback (USB_OTG_CORE_HANDLE *pdev);
uint32_t  USB_OTG_GetCurrentState (USB_OTG_CORE_HANDLE *pdev);

uint32_t STM32_USBO_OTG_ISR_Handler(USB_OTG_CORE_HANDLE *pdev);
/**
  * @}
  */ 


/** @defgroup USB_OTG_Exported_Types
  * @{
  */ 
/**
  * @}
  */ 


/** @defgroup USB_OTG_Exported_Macros
  * @{
  */ 
/**
  * @}
  */ 

/** @defgroup USB_OTG_Exported_Variables
  * @{
  */ 
/**
  * @}
  */ 

/** @defgroup USB_OTG_Exported_FunctionsPrototype
  * @{
  */ 
/**
  * @}
  */ 


#endif //__USB_OTG__


/**
  * @}
  */ 

/**
  * @}
  */ 
/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/

